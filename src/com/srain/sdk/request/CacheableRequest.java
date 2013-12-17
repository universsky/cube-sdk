package com.srain.sdk.request;

import java.net.URI;
import java.net.URISyntaxException;

import android.util.Log;

import com.srain.sdk.request.RequestCacheManager.ICacheable;
import com.srain.sdk.util.Encrypt;

/**
 * <p>
 * A request whose data can be cached, if it has cache data, you can use the cache data to draw the UI at once.
 * 
 * After the request is accomlished, you can update the UI.
 * </p>
 * <p>
 * I belive this will make a better user experience.
 * </p>
 * <p>
 * If the cache data is no expired, this request will not be sent out.
 * </p>
 */
public class CacheableRequest extends Request implements ICacheable {

	private CacheableRequestOnSuccHandler mCacheableRequestHandler;
	private CacheableRequestPreHandler mCacheableRequestPreHandler;

	public CacheableRequest(CacheableRequestPreHandler cacheableRequestPreHandler, CacheableRequestOnSuccHandler cacheableRequestHandler) {
		super(cacheableRequestPreHandler, cacheableRequestHandler);
		mCacheableRequestPreHandler = cacheableRequestPreHandler;
		mCacheableRequestHandler = cacheableRequestHandler;
	}

	/**
	 * Override to query cache before query
	 */
	@Override
	public Request send() {
		prepare();
		RequestCacheManager.getInstance().requestCache(this);
		return this;
	}

	/**
	 * cache data after request
	 */
	@Override
	public void onRequestSucc(JsonData jsonData) {
		super.onRequestSucc(jsonData);

		// cache the data
		if (jsonData != null && jsonData.getRawData() != null && jsonData.length() > 0) {
			RequestCacheManager.getInstance().cacheRequest(this, jsonData);
		} else {
			Log.d("cube_request", "after request, data may be empty, not set to cache");
		}
	}

	// ===========================================================
	// Implements Interface @ICacheable
	// ===========================================================
	@Override
	public void onNoCacheDataAvailable() {
		doQuery();
	}

	@Override
	public void onCacheData(JsonData previousJsonData, boolean outoufDate) {
		mCacheableRequestHandler.onCacheData(previousJsonData, outoufDate);
		if (outoufDate) {
			doQuery();
		}
	}

	@Override
	public int getCacheTime() {
		return mCacheableRequestPreHandler.getCacheTime();
	}

	@Override
	public String getCacheKey() {
		String cachKey = mCacheableRequestPreHandler.getSpecificCacheKey();
		if (cachKey != null && cachKey.length() > 0)
			return cachKey;

		String url = getRequestUrl();
		try {
			URI uri = null;
			uri = new URI(url);
			cachKey = uri.getPath();
			if (cachKey.startsWith("/"))
				cachKey = cachKey.substring(1);
			cachKey = cachKey.replace("/", "-");
			return cachKey;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return Encrypt.md5(url);
	}

	@Override
	public String getAssertInitDataPath() {
		return mCacheableRequestPreHandler.getInitFileAssertPath();
	}

	@Override
	public JsonData processDataFromAssert(JsonData jsonData) {
		return processOriginData(jsonData);
	}
}