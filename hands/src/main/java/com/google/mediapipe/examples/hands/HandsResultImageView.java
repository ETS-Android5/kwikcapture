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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
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


    Matrix matrix = new Matrix();
    matrix.postRotate(180);
    matrix.postScale(-1, 1, width / 2f, height / 2f);
    latest = Bitmap.createBitmap(
            bmInput, 0, 0, width, height, matrix, true);
    Canvas canvas = new Canvas(latest);

    canvas.drawBitmap(latest, new Matrix(), null);
    int numHands = result.multiHandLandmarks().size();
    for (int i = 0; i < numHands; ++i) {
      drawLandmarksOnCanvas(
          result.multiHandLandmarks().get(i).getLandmarkList(),
          result.multiHandedness().get(i).getLabel().equals("Left"),
          canvas,
          width,
          height);
    }
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
    canvas.drawRect(
            (normalizedLandmark.getX() * width) - 55,
            (normalizedLandmark.getY() * height) - 45,
            (normalizedLandmark.getX() * width) + 45,
            (normalizedLandmark.getY() * height) + 95,
            paint);
  }

  /** Updates the image view with the latest {@link HandsResult}. */
  public void update() {
    postInvalidate();
    if (latest != null) {
      setImageBitmap(latest);
    }
  }

  public void captureImage(Context context) throws IOException {
    if (latest != null) {
      File dir = new File("/storage/emulated/0/DCIM"+File.separator, "Kwik Capture");
      // Dir: /storage/emulated/0/DCIM/KwikCapture
      if(!dir.exists()) {
        boolean createdDir = dir.mkdir();
        System.out.println("==== dir created: "+createdDir+", dirPath: "+dir.getAbsolutePath());
      } else {
        System.out.println("==== dir already exists -- dirPath: "+dir.getAbsolutePath());
      }

      File f = new File(dir.getAbsolutePath() + File.separator,
              new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                      .format(System.currentTimeMillis()) + ".jpg");

      boolean createdFile = f.createNewFile();
      System.out.println("==== file created: "+createdFile+", filePath: "+f.getAbsolutePath());

      Bitmap bitmap = latest;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100 /*ignored for PNG*/, bos);
      byte[] bitmapData = bos.toByteArray();

      //write the bytes in file
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(bitmapData);
      Toast.makeText(context, "Saved image!.", Toast.LENGTH_SHORT).show();
      fos.flush();
      fos.close();
    } else {
      System.out.println("=== latest is null");
    }
  }

}
