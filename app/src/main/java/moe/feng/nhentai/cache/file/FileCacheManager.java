package moe.feng.nhentai.cache.file;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import static moe.feng.nhentai.BuildConfig.DEBUG;

public class FileCacheManager {

	private static final String TAG = FileCacheManager.class.getSimpleName();
	
	private static FileCacheManager sInstance;
	
	private File mCacheDir;
	
	public static FileCacheManager getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new FileCacheManager(context);
		}
		
		return sInstance;
	}
	
	private FileCacheManager(Context context) {
		try {
			mCacheDir = context.getExternalCacheDir();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (mCacheDir == null) {
			String cacheAbsDir = "/Android/data" + context.getPackageName() + "/cache/";
			mCacheDir = new File(Environment.getExternalStorageDirectory().getPath() + cacheAbsDir);
		}
	}
	
	public boolean createCacheFromNetwork(String type, String url) {

		if (DEBUG) {
			Log.d(TAG, "requesting cache from " + url);
		}
		
		URL u;
		
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			return false;
		}
		
		HttpURLConnection conn;
		
		try {
			conn = (HttpURLConnection) u.openConnection();
		} catch (IOException e) {
			return false;
		}
		
		conn.setConnectTimeout(5000);

		try {
			if (conn.getResponseCode() != 200) {
				if (url.contains("jpg")) {
					try {
						u = new URL(url.replace("jpg", "png"));
					} catch (MalformedURLException ex) {
						return false;
					}
					try {
						conn = (HttpURLConnection) u.openConnection();
					} catch (IOException ex) {
						return false;
					}
				} else {
					return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			return createCacheFromStrem(type, getCacheName(url), conn.getInputStream());
		} catch (IOException e) {
			return false;
		}
	}
	
	public boolean createCacheFromStrem(String type, String name, InputStream stream) {
		File f = new File(getCachePath(type, name) + "_downloading");
		f.getParentFile().mkdirs();
		f.getParentFile().mkdir();
		
		if (f.exists()) {
			f.delete();
		}
		
		try {
			f.createNewFile();
		} catch (IOException e) {
			return false;
		}
		
		FileOutputStream opt;
		
		try {
			opt = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			return false;
		}
		
		byte[] buf = new byte[512];
		int len = 0;
		
		try {
			while ((len = stream.read(buf)) != -1) {
				opt.write(buf, 0, len);
			}
		} catch (IOException e) {
			return false;
		}
		
		try {
			stream.close();
			opt.close();
		} catch (IOException e) {
			
		}

		f.renameTo(new File(getCachePath(type, name)));

		return true;
	}
	
	// True if the cache downloaded from url exists
	public boolean cacheExistsUrl(String type, String url) {
		return cacheExists(type, getCacheName(url));
	}
	
	public boolean cacheExists(String type, String name) {
		return new File(getCachePath(type, name)).isFile();
	}

	public boolean deleteCacheUrl(String type, String url) {
		return deleteCache(type, getCacheName(url));
	}

	public boolean deleteCache(String type, String name) {
		if (cacheExists(type, name)) {
			return new File(getCachePath(type, name)).delete();
		} else {
			return false;
		}
	}

	public InputStream openCacheStream(String type, String name) {
		try {
			return new FileInputStream(new File(getCachePath(type, name)));
		} catch (IOException e) {
			return null;
		}
	}
	
	public InputStream openCacheStreamUrl(String type, String url) {
		return openCacheStream(type, getCacheName(url));
	}
	
	public Bitmap getBitmap(String type, String name) {
		InputStream ipt = openCacheStream(type, name);
		
		if (ipt == null) return null;
		
		Bitmap ret = BitmapFactory.decodeStream(ipt);
		
		try {
			ipt.close();
		} catch (IOException e) {
			
		}
		
		return ret;
	}
	
	public Bitmap getBitmapUrl(String type, String url) {
		return getBitmap(type, getCacheName(url));
	}

	public File getBitmapFile(String type, String name) {
		return new File(getCachePath(type, name));
	}

	public File getBitmapUrlFile(String type, String url) {
		return getBitmapFile(type, getCacheName(url));
	}

	private String getCacheName(String url) {
		return url.replaceAll("/", ".").replaceAll(":", "");
	}
	
	private String getCachePath(String type, String name) {
		return mCacheDir.getAbsolutePath() + "/" + type + "/" + name + ".cache";
	}

}
