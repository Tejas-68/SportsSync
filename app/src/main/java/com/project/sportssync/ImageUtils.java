package com.project.sportssync;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for image processing and compression
 */
public class ImageUtils {

    private static final int MAX_IMAGE_SIZE = 512; // Max width/height in pixels
    private static final int MAX_FILE_SIZE = 500 * 1024; // 500KB in bytes
    private static final int JPEG_QUALITY = 85; // JPEG compression quality

    /**
     * Compress and resize image from URI
     * @param context Application context
     * @param imageUri URI of the image to compress
     * @return Compressed image as byte array, or null if error
     */
    public static byte[] compressImage(Context context, Uri imageUri) {
        try {
            // CRITICAL FIX: Use inJustDecodeBounds to calculate sample size BEFORE loading full image
            // This prevents OutOfMemoryError on large images (12MP+ from modern cameras)
            
            // Step 1: Get image dimensions without loading the full bitmap
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;
            
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // Only decode dimensions, not pixels
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            // Get original dimensions
            int imageWidth = options.outWidth;
            int imageHeight = options.outHeight;
            
            // Step 2: Calculate optimal sample size to downsample the image
            int inSampleSize = calculateInSampleSize(imageWidth, imageHeight, MAX_IMAGE_SIZE);
            
            // Step 3: Now decode the actual bitmap with downsampling
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;
            
            options.inJustDecodeBounds = false; // Now decode the actual pixels
            options.inSampleSize = inSampleSize; // Apply downsampling
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Use less memory (2 bytes/pixel vs 4)
            
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (originalBitmap == null) return null;

            // Fix orientation
            Bitmap rotatedBitmap = fixOrientation(context, imageUri, originalBitmap);

            // Resize to exact max dimensions (fine-tuning after downsampling)
            Bitmap resizedBitmap = resizeBitmap(rotatedBitmap, MAX_IMAGE_SIZE);

            // Compress to JPEG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int quality = JPEG_QUALITY;
            
            // Compress with reducing quality until under max file size
            do {
                outputStream.reset();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                quality -= 5;
            } while (outputStream.size() > MAX_FILE_SIZE && quality > 10);

            byte[] compressedData = outputStream.toByteArray();
            outputStream.close();

            // Clean up bitmaps to free memory
            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle();
            }
            resizedBitmap.recycle();
            originalBitmap.recycle();

            return compressedData;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            // Even with downsampling, log if OOM still occurs
            android.util.Log.e("ImageUtils", "OutOfMemoryError during image compression", e);
            return null;
        }
    }
    
    /**
     * Calculate optimal sample size for downsampling large images
     * This prevents loading massive images into memory
     * 
     * @param width Original image width
     * @param height Original image height
     * @param reqSize Required maximum size
     * @return Sample size (power of 2) for inSampleSize
     */
    private static int calculateInSampleSize(int width, int height, int reqSize) {
        int inSampleSize = 1;
        
        if (width > reqSize || height > reqSize) {
            final int halfWidth = width / 2;
            final int halfHeight = height / 2;
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested size
            while ((halfWidth / inSampleSize) >= reqSize && (halfHeight / inSampleSize) >= reqSize) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }

    /**
     * Resize bitmap to fit within max dimensions while maintaining aspect ratio
     */
    private static Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float scale = Math.min(
                (float) maxSize / width,
                (float) maxSize / height
        );

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Fix image orientation based on EXIF data
     */
    private static Bitmap fixOrientation(Context context, Uri imageUri, Bitmap bitmap) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return bitmap;

            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            inputStream.close();

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1, -1);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true
            );

        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    /**
     * Generate thumbnail from compressed image data
     */
    public static byte[] generateThumbnail(byte[] imageData) {
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            if (bitmap == null) return null;

            // Create 128x128 thumbnail
            Bitmap thumbnail = resizeBitmap(bitmap, 128);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] thumbnailData = outputStream.toByteArray();
            outputStream.close();

            bitmap.recycle();
            thumbnail.recycle();

            return thumbnailData;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
