package com.zzc.network.response;

import com.zzc.baselib.ui.listener.IProgressDialog;
import com.zzc.baselib.util.NetworkUtils;
import com.zzc.network.BuildConfig;
import com.zzc.network.cache.CacheStrategyUtil;

import io.reactivex.functions.Consumer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Roye
 * @date 2018/9/6
 */
public abstract class PriorityCacheResponseDataCallback<Result extends IHttpResponse<Data>, Data> implements Callback<Result>, SupportResponseLifecycle<Result, Data> {

    private SupportProcedure procedure;
    private int requestCount;
    private Result lastCacheResult;

    public PriorityCacheResponseDataCallback() {
        procedure = new SupportProcedure();
        procedure.setResponseLifecycle(this);
    }

    public void setProgressDialog(IProgressDialog progressDialog) {
        procedure.setProgressDialog(progressDialog);
    }

    public void setProgressDialog(IProgressDialog progressDialog, String loadingText) {
        procedure.setProgressDialog(progressDialog, loadingText);
    }

    @Override
    public void onStart() {
        procedure.showLoading();
    }

    @Override
    public void onResponse(Call<Result> call, final Response<Result> response) {
        if (response.code() >= 400 || response.body() == null) {
            onFinish();
            return;
        }
        okhttp3.Response networkResopnse = response.raw().networkResponse();
        if (networkResopnse != null) {
            if (lastCacheResult == null) {
                procedure.handleResponse(response.body());
                onFinish();
            } else {
                CacheStrategyUtil.checkSame(lastCacheResult, response.body(), new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        if ("n".equals(s)) {
                            procedure.handleResponse(response.body());
                        }
                        onFinish();
                    }
                });
            }
        } else {
            lastCacheResult = response.body();

            procedure.handleResponse(response.body());
            onFinish();

            if (requestCount == 0) {
                requestCount++;
                if (NetworkUtils.isAvailable()) {
                    call.clone().enqueue(this);
                }
            }
        }
    }

    @Override
    public void onFinish() {
        procedure.hideLoading();
    }

    @Override
    public void onFailed(Result result) {
        DefaultResponseCodeHandle.handle(result);
        if (result.getMsg() != null) {
        }
    }

    @Override
    public void onFailure(Call<Result> call, Throwable t) {
        onError(t);
    }

    public void onError(Throwable e) {
        procedure.handle(e);
        onFinish();
    }
}