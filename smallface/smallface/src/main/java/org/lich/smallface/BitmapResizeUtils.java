package org.lich.smallface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

/**
 * bitmap压缩类(给定res目录下图片的id,给定高和宽)
 */
public class BitmapResizeUtils {

	private BitmapResizeUtils() {
	};

	/**
	 * 图片压缩计算Options，用于压缩图片 <br>
	 * <b>说明</b> 使用方法：
	 * 首先你要将Options的inJustDecodeBounds属性设置为true，BitmapFactory.decode一次图片 。
	 * 然后将Options连同期望的宽度和高度一起传递到到本方法中。
	 * 之后再使用本方法的返回值做参数调用BitmapFactory.decode创建图片。
	 * 
	 * <br>
	 * <b>说明</b> BitmapFactory创建bitmap会尝试为已经构建的bitmap分配内存
	 * ，这时就会很容易导致OOM出现。为此每一种创建方法都提供了一个可选的Options参数
	 * ，将这个参数的inJustDecodeBounds属性设置为true就可以让解析方法禁止为bitmap分配内存
	 * ，返回值也不再是一个Bitmap对象， 而是null。虽然Bitmap是null了，但是Options的outWidth、
	 * outHeight和outMimeType属性都会被赋值。
	 * 
	 * @param options
	 *            未设置值BitmapFactory.Options
	 * 
	 * @param reqWidth
	 *            目标宽度,这里的宽高只是阀值，实际显示的图片将小于等于这个值
	 * @param reqHeight
	 *            目标高度,这里的宽高只是阀值，实际显示的图片将小于等于这个值
	 * 
	 * @return 经过计算得出的BitmapFactory.Options
	 */
	public static Options calculateInSampleSize(
			final Options options, final int reqWidth,
			final int reqHeight) {
		// 源图片的高度和宽度
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		if (height > reqHeight || width > reqWidth) {
			// 计算出实际宽高和目标宽高的比率
			final int heightRatio = Math.round(((float) height/ (float) reqHeight+0.3f));
			final int widthRatio = Math.round(((float) width / (float) reqWidth+0.3f));
			// 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
			// 一定都会大于等于目标的宽和高。
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		// 设置压缩比例
		options.inSampleSize = inSampleSize;
		options.inJustDecodeBounds = false;
		return options;
	}

	/**
	 * 通过id获得目标宽高的压缩图片
	 * 
	 * @param res
	 *            资源源
	 * @param resId
	 *            资源id
	 * @param width
	 *            目标宽度
	 * @param height
	 *            目标高度
	 * @return Bitmap
	 */
	public static Bitmap resizeResources(Resources res, int resId, int width,
			int height) {
		Options options = new Options();
		options.inJustDecodeBounds = true;// 不加载bitmap到内存中
		// 获取源文件宽高
		BitmapFactory.decodeResource(res, resId, options);
		// 计算出合理的Options
		options = calculateInSampleSize(options, width, height);
		InputStream is = res.openRawResource(resId);
		Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * 根据文件路径获得目标宽高的压缩图片
	 * 
	 * @param path
	 *            文件路径
	 * @param width
	 *            目标宽度
	 * @param height
	 *            目标高度
	 * @return Bitmap
	 * @throws IOException
	 *             FileNofindExcetption
	 */
	public static Bitmap resizeFile(String path, int width, int height)
			throws IOException {
		Options options = new Options();
		options.inJustDecodeBounds = true;// 不加载bitmap到内存中
		BitmapFactory.decodeFile(path, options);
		options = calculateInSampleSize(options, width, height);
		InputStream is = new FileInputStream(path);
		Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
		if (is != null) {
			is.close();
		}
		return bitmap;
	}
	/**
	 * 质量压缩，根据Bitmap质量，压缩
	 * 
	 * @param image
	 *            较大的Bitmap
	 * @param size
	 *            目标图片质量大小(kb)
	 * @return JPEG格式的Bitmap
	 */
	public static Bitmap compressImage(Bitmap image, int size) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// 0-100。0意义压缩体积小,100表示质量最佳。一些格式，PNG是无损的，会忽略质量压缩设置，把压缩后的数据存放到baos中
		image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		int quality = 85;
		while (baos.toByteArray().length / 1024 > size) { // 循环判断如果压缩后图片是否大于200kb,大于继续压缩
			// 重置baos
			baos.reset();
			image.compress(Bitmap.CompressFormat.JPEG, quality, baos);// 这里压缩quality%，把压缩后的数据存放到baos中
			quality -= 10;// 每次压缩10%
		}
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
		return bitmap;
	}

	// 图片按比例大小压缩方法（根据Bitmap图片压缩）
		public static Bitmap comp(Bitmap image,int width,int height) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			image.compress(Bitmap.CompressFormat.PNG, 100, baos);
			ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
			Options options = new Options();
			options.inJustDecodeBounds = true;
			Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, options);
			options = calculateInSampleSize(options, width, height);
			// 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
			isBm = new ByteArrayInputStream(baos.toByteArray());
			bitmap = BitmapFactory.decodeStream(isBm, null, options);
			if(isBm!=null){
				try {
					isBm.close();
				} catch (IOException e) {e.printStackTrace();}
			}
			return bitmap;
		}
	// Bitmap → Drawable
	public static Drawable convertBitmapToDrawable(Context context,
			Bitmap bitmap) {
		BitmapDrawable bd = new BitmapDrawable(context.getResources(), bitmap);
		// 因为BtimapDrawable是Drawable的子类，最终直接使用bd对象即可。
		return bd;
	}

	// Drawable → Bitmap
	public static Bitmap convertDrawableToBitmapSimple(Drawable drawable) {
		BitmapDrawable bd = (BitmapDrawable) drawable;
		return bd.getBitmap();
	}

	/**
	 * BitMap 转InputStream(PNG格式无损)
	 */
	public static InputStream Bitmap2IS(Bitmap bm) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
		InputStream sbs = new ByteArrayInputStream(baos.toByteArray());
		return sbs;
	}

	/**
	 * base64 bitmap -->转byte数组-->base64
	 * 
	 * @param bm
	 */
	public static String bitmapToBase64(Bitmap bitmap) {

		String result = null;
		ByteArrayOutputStream baos = null;
		try {
			if (bitmap != null) {
				baos = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

				baos.flush();
				baos.close();

				byte[] bitmapBytes = baos.toByteArray();
				result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (baos != null) {
					baos.flush();
					baos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
}
