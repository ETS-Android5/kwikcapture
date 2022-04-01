// Copyright 2021 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.examples.hands;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutions.hands.HandLandmark;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** An ImageView implementation for displaying {@link HandsResult}. */
public class HandsResultImageView extends AppCompatImageView {
  private static final String TAG = "HandsResultImageView";

  private static final int LEFT_HAND_CONNECTION_COLOR = Color.parseColor("#30FF30");
  private static final int RIGHT_HAND_CONNECTION_COLOR = Color.parseColor("#FF3030");
  private static final int CONNECTION_THICKNESS = 8; // Pixels
  private static final int LEFT_HAND_HOLLOW_CIRCLE_COLOR = Color.parseColor("#30FF30");
  private static final int RIGHT_HAND_HOLLOW_CIRCLE_COLOR = Color.parseColor("#FF3030");
  private static final int HOLLOW_CIRCLE_WIDTH = 5; // Pixels
  private static final int LEFT_HAND_LANDMARK_COLOR = Color.parseColor("#FF3030");
  private static final int RIGHT_HAND_LANDMARK_COLOR = Color.parseColor("#30FF30");
  private static final int LANDMARK_RADIUS = 10; // Pixels
  private Bitmap latest;
  private HandsResult lResult;

  public HandsResultImageView(Context context) {
    super(context);
    setScaleType(AppCompatImageView.ScaleType.FIT_CENTER);
  }

  /**
   * Sets a {@link HandsResult} to render.
   *
   * @param result a {@link HandsResult} object that contains the solution outputs and the input
   *     {@link Bitmap}.
   */
  public void setHandsResult(HandsResult result) {
    if (result == null) {
      System.out.println("=== setHandsResult: == null");
      return;
    }
    Bitmap bmInput = result.inputBitmap();
    int width = bmInput.getWidth();
    int height = bmInput.getHeight();

//    latest = Bitmap.createBitmap(width, height, bmInput.getConfig()); // old
    lResult = result;

    Matrix matrix = new Matrix();
    matrix.postRotate(180);
    matrix.postScale(-1, 1, width / 2f, height / 2f);
    latest = Bitmap.createBitmap(
            bmInput, 0, 0, width, height, matrix, true);

    // ==== DRAW RECTANGLES ON IMAGE ====
//    Canvas canvas = new Canvas(latest);
//    canvas.drawBitmap(latest, new Matrix(), null);
//    int numHands = result.multiHandLandmarks().size();
//
//    if(numHands > 0) {
//        for (int i = 0; i < numHands; ++i) {
//          drawLandmarksOnCanvas(
//                  result.multiHandLandmarks().get(i).getLandmarkList(),
//                  result.multiHandedness().get(i).getLabel().equals("Left"),
//                  canvas,
//                  width,
//                  height);
//        }
//    }

  }

  private void drawLandmarksOnCanvas(List<NormalizedLandmark> handLandmarkList, boolean isLeftHand,
      Canvas canvas, int width, int height) {
    // Draw connections.
//    for (Hands.Connection c : Hands.HAND_CONNECTIONS) {
//      Paint connectionPaint = new Paint();
//      connectionPaint.setColor(
//          isLeftHand ? LEFT_HAND_CONNECTION_COLOR : RIGHT_HAND_CONNECTION_COLOR);
//      connectionPaint.setStrokeWidth(CONNECTION_THICKNESS);
//      NormalizedLandmark start = handLandmarkList.get(c.start());
//      NormalizedLandmark end = handLandmarkList.get(c.end());
//      canvas.drawLine(
//          start.getX() * width,
//          start.getY() * height,
//          end.getX() * width,
//          end.getY() * height,
//          connectionPaint);
//    }
    Paint landmarkPaint = new Paint();
    landmarkPaint.setColor(isLeftHand ? LEFT_HAND_LANDMARK_COLOR : RIGHT_HAND_LANDMARK_COLOR);
    // Draws landmarks.
//    for (LandmarkProto.NormalizedLandmark landmark : handLandmarkList) {
//      canvas.drawCircle(
//          landmark.getX() * width, landmark.getY() * height, LANDMARK_RADIUS, landmarkPaint);
//    }
    // Draws hollow circles around landmarks.
    landmarkPaint.setColor(
        isLeftHand ? LEFT_HAND_HOLLOW_CIRCLE_COLOR : RIGHT_HAND_HOLLOW_CIRCLE_COLOR);
    landmarkPaint.setStrokeWidth(HOLLOW_CIRCLE_WIDTH);
    landmarkPaint.setStyle(Paint.Style.STROKE);

      NormalizedLandmark indexFingerTipLandmark =
              handLandmarkList.get(HandLandmark.INDEX_FINGER_TIP);
      NormalizedLandmark middleFingerTipLandmark =
              handLandmarkList.get(HandLandmark.MIDDLE_FINGER_TIP);
      NormalizedLandmark ringFingerTipLandmark =
              handLandmarkList.get(HandLandmark.RING_FINGER_TIP);
      NormalizedLandmark pinkyTipLandmark =
              handLandmarkList.get(HandLandmark.PINKY_TIP);

    drawRectOnImage(canvas, indexFingerTipLandmark, landmarkPaint, width, height);
    drawRectOnImage(canvas, middleFingerTipLandmark, landmarkPaint, width, height);
    drawRectOnImage(canvas, ringFingerTipLandmark, landmarkPaint, width, height);
    drawRectOnImage(canvas, pinkyTipLandmark, landmarkPaint, width, height);

//    for (LandmarkProto.NormalizedLandmark landmark : handLandmarkList) {
//      canvas.drawCircle(
//              landmark.getX() * width,
//              landmark.getY() * height,
//              40, // LANDMARK_RADIUS + HOLLOW_CIRCLE_WIDTH
//              landmarkPaint);
//    }
  }

  private void drawRectOnImage(Canvas canvas, NormalizedLandmark normalizedLandmark,
                     Paint paint, int width, int height) {

    System.out.println("=== left: "+((normalizedLandmark.getX() * width) - 65)
            +", top: "+((normalizedLandmark.getY() * height) - 55)
            +", right: "+((normalizedLandmark.getX() * width) + 55)
            +", bottom: "+((normalizedLandmark.getY() * height) + 105));

    canvas.drawRect(
            (normalizedLandmark.getX() * width) - 65,
            (normalizedLandmark.getY() * height) - 55,
            (normalizedLandmark.getX() * width) + 55,
            (normalizedLandmark.getY() * height) + 105,
            paint);
  }

  /** Updates the image view with the latest {@link HandsResult}. */
  public void update() {
    postInvalidate();
    if (latest != null) {
      setImageBitmap(latest);
    }
  }

  public Bitmap toGrayscale(Bitmap bmpOriginal) {
    int width, height;
    height = bmpOriginal.getHeight();
    width = bmpOriginal.getWidth();
  // == RGB_565: 3, ARGB_8888: 2, ARGB_4444: 1
    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    Canvas c = new Canvas(bmpGrayscale);
    Paint paint = new Paint();
    ColorMatrix cm = new ColorMatrix();
    cm.setSaturation(0);
    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
    paint.setColorFilter(f);
    c.drawBitmap(bmpOriginal, 0, 0, paint);
    return bmpGrayscale;
  }

  /**
   *
   * @param bmp input bitmap
   * @param contrast 0..10 1 -- 1 is default
   * @param brightness -255..255 -- 0 is default
   * @return new bitmap
   */
  public static Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness) {
    ColorMatrix cm = new ColorMatrix(new float[]
            {
                    contrast, 0, 0, 0, brightness,
                    0, contrast, 0, 0, brightness,
                    0, 0, contrast, 0, brightness,
                    0, 0, 0, 1, 0
            });

    Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

    Canvas canvas = new Canvas(ret);

    Paint paint = new Paint();
    paint.setColorFilter(new ColorMatrixColorFilter(cm));
    canvas.drawBitmap(bmp, 0, 0, paint);

    return ret;
  }

  private int hueChange(int startpixel,int deg){
    float[] hsv = new float[3];       //array to store HSV values
    Color.colorToHSV(startpixel,hsv); //get original HSV values of pixel
    hsv[0]=hsv[0]+deg;                //add the shift to the HUE of HSV array
    hsv[0]=hsv[0]%360;                //confines hue to values:[0,360]
    return Color.HSVToColor(Color.alpha(startpixel),hsv);
  }
  private Bitmap adjustedHue(Bitmap o, int deg)
  {
    Bitmap srca = o;
    Bitmap bitmap = srca.copy(Bitmap.Config.ARGB_8888, true);
    for(int x = 0;x < bitmap.getWidth();x++)
      for(int y = 0;y < bitmap.getHeight();y++){
        int newPixel = hueChange(bitmap.getPixel(x,y),deg);
        bitmap.setPixel(x, y, newPixel);
      }

    return bitmap;
  }

  public Bitmap sharpen(Bitmap src, double weight) {
    double[][] SharpConfig = new double[][] {
            { 0 , -2    , 0  },
            { -2, weight, -2 },
            { 0 , -2    , 0  }
    };
    ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
    convMatrix.applyConfig(SharpConfig);
    convMatrix.Factor = weight - 8;
    return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
  }

  public Bitmap compress(Bitmap yourBitmap){
    //converted into webp into lowest quality
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    yourBitmap.compress(Bitmap.CompressFormat.WEBP,0,stream);//0=lowest, 100=highest quality
    byte[] byteArray = stream.toByteArray();


    //convert your byteArray into bitmap
    return BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
  }

  public void createFileAndSaveImage(Context context, NormalizedLandmark landmark, boolean isLeft,
                                     File dir, Bitmap bitmap, String uniqueId, String setNo,
                                     int fingerNo) throws IOException {
    Toast saveFileToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
    String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis());
    File f = new File(dir.getAbsolutePath() + File.separator,
            "kwikCapture_"+uniqueId+"_Set-"+setNo+"_Finger-"+fingerNo+"_" + date + ".png");

    boolean createdFile = f.createNewFile();
    System.out.println("==== file created: "+createdFile+", filePath: "+f.getAbsolutePath());

    float width = bitmap.getWidth();
    float height = bitmap.getHeight();

    System.out.println("=== bitmap: w: "+width+", h: "+height);
    int left = (int) (landmark.getX() * width) - 70; // x
    int top = (int) (landmark.getY() *  height) - 60; // y
    int right = 120; // width
    int bottom = 170; // height
    System.out.println("=== bitmap: left: "+left+", top: "+top+", right: "+right+", bottom: "+bottom+ ", fingerNo: "+fingerNo);

    if(left + right > width) {
      right = (int) (right - (width - (left+right)));
    }

//    Matrix matrix = new Matrix();
//    matrix.postScale((float) 500 / bitmap.getWidth(), (float) 500 / bitmap.getHeight());
    Bitmap cBitmap = Bitmap.createBitmap(bitmap, left, top, right, bottom); // bitmap, x, y, width, height
    Bitmap scaledBitmap = Bitmap.createScaledBitmap(cBitmap, 350, 500, false);
//    cBitmap.setPixel(500, 500, 0);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100 /*ignored for PNG*/, bos);
    byte[] bitmapData = bos.toByteArray();

    //write the bytes in file
    FileOutputStream fos = new FileOutputStream(f);
    fos.write(bitmapData);

    fos.flush();
    fos.close();

    if(fingerNo == 10 || fingerNo == 5) {
      saveFileToast.setText("Files created successfully!");
      saveFileToast.show();
    }
  }

  public void captureImage(Context context, String uniqueId, String setNo) throws IOException {
    Toast statusToast = Toast.makeText(context, "", Toast.LENGTH_LONG);
    Toast doneToast = Toast.makeText(context, "", Toast.LENGTH_LONG);

    if (latest != null) {
      File dir = new File("/storage/emulated/0/DCIM"+File.separator, "Kwik Capture");
      // Dir: /storage/emulated/0/DCIM/KwikCapture
      if(!dir.exists()) {
        boolean createdDir = dir.mkdir();
        System.out.println("==== dir created: "+createdDir+", dirPath: "+dir.getAbsolutePath());
      } else {
        System.out.println("==== dir already exists -- dirPath: "+dir.getAbsolutePath());
      }

      File dir2 = new File("/storage/emulated/0/DCIM/Kwik Capture"+File.separator, uniqueId);
      // Dir: /storage/emulated/0/DCIM/KwikCapture
      if(!dir2.exists()) {
        boolean createdDir2 = dir2.mkdir();
        System.out.println("==== dir2 created: "+createdDir2+", dirPath: "+dir2.getAbsolutePath());
      } else {
        System.out.println("==== dir2 already exists -- dirPath: "+dir2.getAbsolutePath());
      }

      List<NormalizedLandmark> handLandmarkList;
      boolean isLeftHand;
      if(lResult.multiHandLandmarks().size() == 1) {

        statusToast.setText("Processing images...");
        statusToast.setDuration(Toast.LENGTH_LONG);
        statusToast.show();

        handLandmarkList = lResult.multiHandLandmarks().get(0).getLandmarkList();
        isLeftHand = lResult.multiHandedness().get(0).getLabel().equals("Left");

        System.out.println("==== isLeft: "+isLeftHand+", label: "+lResult.multiHandedness().get(0).getLabel());

        NormalizedLandmark indexFingerTipLandmark = handLandmarkList.get(HandLandmark.INDEX_FINGER_TIP);
        NormalizedLandmark middleFingerTipLandmark = handLandmarkList.get(HandLandmark.MIDDLE_FINGER_TIP);
        NormalizedLandmark ringFingerTipLandmark = handLandmarkList.get(HandLandmark.RING_FINGER_TIP);
        NormalizedLandmark pinkyTipLandmark = handLandmarkList.get(HandLandmark.PINKY_TIP);

        latest = toGrayscale(latest);
//        latest = sharpen(latest, 10);
//        latest = changeBitmapContrastBrightness(latest, 4, -20);
        latest = adjustedHue(latest, 180);

        statusToast.setText("Creating files...");
        statusToast.show();

        createFileAndSaveImage(context, indexFingerTipLandmark, isLeftHand, dir2, latest, uniqueId, setNo, isLeftHand ? 2 : 7);
        createFileAndSaveImage(context, middleFingerTipLandmark, isLeftHand, dir2, latest, uniqueId, setNo, isLeftHand ? 3 : 8);
        createFileAndSaveImage(context, ringFingerTipLandmark, isLeftHand, dir2, latest, uniqueId, setNo, isLeftHand ? 4 : 9);
        createFileAndSaveImage(context, pinkyTipLandmark, isLeftHand, dir2, latest, uniqueId, setNo, isLeftHand ? 5 : 10);

        doneToast.setText("Completed saving images!");
        doneToast.show();

      } else {
        statusToast.setText("Palm not detected, try again!");
        statusToast.show();
      }
      System.out.println("==== DONE ====");

    } else {
      System.out.println("=== latest is null");
    }
  }

}
