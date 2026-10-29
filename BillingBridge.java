package com.solenlabs.drava;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

/**
 * Exposed to the page as window.DravaAndroid.
 *
 * The page calls purchase()/restorePurchases(); every owned purchase is handed back
 * to window.onDravaPurchase({token, productId}). The page verifies it with the
 * drava-account Edge Function and only then calls acknowledge(token).
 */
public class BillingBridge implements PurchasesUpdatedListener {

    private static final String ARCHIVES = "drava_archives";

    private final Activity activity;
    private final WebView webView;
    private final BillingClient billing;
    private final String deviceId;

    public BillingBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.deviceId = makeDeviceId(activity);
        this.billing = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        connect(null);
    }

    // ------------------------------------------------------------------ JS API

    @JavascriptInterface
    public String getDeviceId() {
        return deviceId;
    }

    /** productId: "drava_operative" (basePlanId "monthly" / "yearly") or "drava_archives" (basePlanId ""). */
    @JavascriptInterface
    public void purchase(final String productId, final String basePlanId) {
        connect(() -> {
            String type = ARCHIVES.equals(productId) ? BillingClient.ProductType.INAPP : BillingClient.ProductType.SUBS;
            QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                    .setProductList(Collections.singletonList(
                            QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId(productId)
                                    .setProductType(type)
                                    .build()))
                    .build();
            billing.queryProductDetailsAsync(params, (result, detailsResult) -> {
                List<ProductDetails> list = detailsResult.getProductDetailsList();
                if (result.getResponseCode() != BillingClient.BillingResponseCode.OK || list == null || list.isEmpty()) {
                    sendError("product_unavailable");
                    return;
                }
                ProductDetails pd = list.get(0);
                BillingFlowParams.ProductDetailsParams.Builder pdp =
                        BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd);
                if (BillingClient.ProductType.SUBS.equals(type)) {
                    String token = pickOffer(pd, basePlanId);
                    if (token == null) {
                        sendError("plan_unavailable");
                        return;
                    }
                    pdp.setOfferToken(token);
                }
                final BillingFlowParams flow = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Collections.singletonList(pdp.build()))
                        .setObfuscatedAccountId(deviceId)
                        .build();
                activity.runOnUiThread(() -> billing.launchBillingFlow(activity, flow));
            });
        });
    }

    /** Re-sends every active purchase to the page (called on every app start). */
    @JavascriptInterface
    public void restorePurchases() {
        connect(() -> {
            for (String type : new String[]{BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP}) {
                billing.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder().setProductType(type).build(),
                        (result, purchases) -> {
                            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                for (Purchase p : purchases) sendPurchase(p);
                            }
                        });
            }
        });
    }

    /** Called by the page after the server has verified the purchase. */
    @JavascriptInterface
    public void acknowledge(final String purchaseToken) {
        connect(() -> billing.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build(),
                result -> { /* nothing to do — Play refunds unacknowledged purchases after 3 days */ }));
    }

    @JavascriptInterface
    public void copy(String text) {
        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Drava", text));
    }

    // ------------------------------------------------------------------ Play callbacks

    @Override
    public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        int code = result.getResponseCode();
        if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) sendPurchase(p);
        } else if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
            sendError("canceled");
        } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            restorePurchases();
        } else {
            sendError("billing_" + code);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void connect(final Runnable then) {
        if (billing.isReady()) {
            if (then != null) then.run();
            return;
        }
        billing.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (then != null) then.run();
                } else if (then != null) {
                    sendError("billing_unavailable");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() handles reconnects on the next call
            }
        });
    }

    private static String pickOffer(ProductDetails pd, String basePlanId) {
        List<ProductDetails.SubscriptionOfferDetails> offers = pd.getSubscriptionOfferDetails();
        if (offers == null) return null;
        String fallback = null;
        for (ProductDetails.SubscriptionOfferDetails o : offers) {
            if (!o.getBasePlanId().equals(basePlanId)) continue;
            if (o.getOfferId() == null) return o.getOfferToken(); // the plain base plan
            if (fallback == null) fallback = o.getOfferToken();
        }
        return fallback;
    }

    private void sendPurchase(Purchase p) {
        if (p.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
        try {
            JSONObject o = new JSONObject();
            o.put("token", p.getPurchaseToken());
            o.put("productId", p.getProducts().isEmpty() ? "" : p.getProducts().get(0));
            o.put("acknowledged", p.isAcknowledged());
            callJs(o);
        } catch (Exception ignored) {
        }
    }

    private void sendError(String error) {
        try {
            JSONObject o = new JSONObject();
            o.put("error", error);
            callJs(o);
        } catch (Exception ignored) {
        }
    }

    private void callJs(final JSONObject payload) {
        final String js = "window.onDravaPurchase&&window.onDravaPurchase(" + payload.toString() + ")";
        activity.runOnUiThread(() -> webView.evaluateJavascript(js, null));
    }

    /** Stable per install+signing key; hashed so the raw ANDROID_ID never leaves the device. */
    private static String makeDeviceId(Context ctx) {
        String raw = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (raw == null) raw = "unknown";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("drava:" + raw).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder("a-");
            for (int i = 0; i < 16; i++) sb.append(String.format("%02x", d[i]));
            return sb.toString();
        } catch (Exception e) {
            return "a-" + raw.replaceAll("[^A-Za-z0-9]", "");
        }
    }
}
