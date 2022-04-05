// Copyright 2022 kwikCapture author.
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

package com.google.mediapipe.apps.hands;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;
// ContentResolver dependency
import com.google.mediapipe.formats.proto.LandmarkProto.Landmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutioncore.VideoInput;
import com.google.mediapipe.solutions.hands.HandLandmark;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsOptions;
import com.google.mediapipe.solutions.hands.HandsResult;
import java.io.IOException;
import java.io.InputStream;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.dart.DartExecutor;

/** Main activity of MediaPipe Hands app. */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";
  final Context context = this;

  private Hands hands;
  // Run the pipeline and the model inference on GPU or CPU.
  private static final boolean RUN_ON_GPU = true;

  private enum InputSource {
    UNKNOWN,
    IMAGE,
    VIDEO,
    CAMERA,
  }
  private InputSource inputSource = InputSource.UNKNOWN;

  // Image demo UI and image loader components.
  private ActivityResultLauncher<Intent> imageGetter;
  private HandsResultImageView imageView;
  // Video demo UI and video loader components.
  private VideoInput videoInput;
  private ActivityResultLauncher<Intent> videoGetter;
  // Live camera demo UI and camera components.
  private KCCameraInput cameraInput;
  private SolutionGlSurfaceView<HandsResult> glSurfaceView;

  public FlutterEngine flutterEngine;

  private CameraManager camManager;
  ImageButton captureImageButton;
  Button stopCameraButton;
  Button startCameraButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    startCameraButton = findViewById(R.id.button_start_camera);
    stopCameraButton = findViewById(R.id.button_stop_camera);
    captureImageButton = findViewById(R.id.button_capture_image);

    setupStaticImageDemoUiComponents();
    setupVideoDemoUiComponents();
    setupLiveDemoUiComponents();
    stopLiveDemoUiComponent();
//    setupFlutterScreenInit();
    setupCaptureImageUiComponents();

    // Instantiate a FlutterEngine.
    flutterEngine = new FlutterEngine(this);

    // Start executing Dart code to pre-warm the FlutterEngine.
    flutterEngine.getDartExecutor().executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
    );

    // Cache the FlutterEngine to be used by FlutterActivity.
    FlutterEngineCache
            .getInstance()
            .put("my_engine_id", flutterEngine);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (inputSource == InputSource.CAMERA) {
      // Restarts the camera and the opengl surface rendering.
      cameraInput = new KCCameraInput(this);
      cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
      glSurfaceView.post(this::startCamera);
      glSurfaceView.setVisibility(View.VISIBLE);
    } else if (inputSource == InputSource.VIDEO) {
      videoInput.resume();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (inputSource == InputSource.CAMERA) {
      glSurfaceView.setVisibility(View.GONE);
      cameraInput.close();
    } else if (inputSource == InputSource.VIDEO) {
      videoInput.pause();
    }
  }

  private Bitmap downscaleBitmap(Bitmap originalBitmap) {
    double aspectRatio = (double) originalBitmap.getWidth() / originalBitmap.getHeight();
    int width = imageView.getWidth();
    int height = imageView.getHeight();
    if (((double) imageView.getWidth() / imageView.getHeight()) > aspectRatio) {
      width = (int) (height * aspectRatio);
    } else {
      height = (int) (width / aspectRatio);
    }
    return Bitmap.createScaledBitmap(originalBitmap, width, height, false);
  }

  private Bitmap rotateBitmap(Bitmap inputBitmap, InputStream imageData) throws IOException {
    int orientation =
        new ExifInterface(imageData)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    if (orientation == ExifInterface.ORIENTATION_NORMAL) {
      return inputBitmap;
    }
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
      default:
        matrix.postRotate(0);
    }
    return Bitmap.createBitmap(
        inputBitmap, 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true);
  }

  /** Sets up the UI components for the static image demo. */
  private void setupStaticImageDemoUiComponents() {
    // The Intent to access gallery and read images as bitmap.
    imageGetter =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              Intent resultIntent = result.getData();
              if (resultIntent != null) {
                if (result.getResultCode() == RESULT_OK) {
                  Bitmap bitmap = null;
                  try {
                    bitmap =
                        downscaleBitmap(
                            MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(), resultIntent.getData()));
                  } catch (IOException e) {
                    Log.e(TAG, "Bitmap reading error:" + e);
                  }
                  try {
                    InputStream imageData =
                        this.getContentResolver().openInputStream(resultIntent.getData());
                    bitmap = rotateBitmap(bitmap, imageData);
                  } catch (IOException e) {
                    Log.e(TAG, "Bitmap rotation error:" + e);
                  }
                  if (bitmap != null) {
                    hands.send(bitmap);
                  }
                }
              }
            });
//    Button loadImageButton = findViewById(R.id.button_load_picture);
//    loadImageButton.setOnClickListener(
//        v -> {
//          if (inputSource != InputSource.IMAGE) {
//            stopCurrentPipeline();
//            setupStaticImageModePipeline();
//          }
//          // Reads images from gallery.
//          Intent pickImageIntent = new Intent(Intent.ACTION_PICK);
//          pickImageIntent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
//          imageGetter.launch(pickImageIntent);
//        });
    imageView = new HandsResultImageView(this);
  }

  /** Sets up core workflow for static image mode. */
  private void setupStaticImageModePipeline() {
    this.inputSource = InputSource.IMAGE;
    // Initializes a new MediaPipe Hands solution instance in the static image mode.
    hands =
        new Hands(
            this,
            HandsOptions.builder()
                .setStaticImageMode(true)
                .setMaxNumHands(2)
                .setRunOnGpu(RUN_ON_GPU)
                .build());

    // Connects MediaPipe Hands solution to the user-defined HandsResultImageView.
    hands.setResultListener(
        handsResult -> {
//          logIndexFingerTipLandmark(handsResult);
          imageView.setHandsResult(handsResult);
          runOnUiThread(() -> imageView.update());
        });
    hands.setErrorListener((message, e) -> Log.e(TAG, "Kwik Capture error:" + message));

    // Updates the preview layout.
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    frameLayout.removeAllViewsInLayout();
    imageView.setImageDrawable(null);
    frameLayout.addView(imageView);
    imageView.setVisibility(View.VISIBLE);
  }

  /** Sets up the UI components for the video demo. */
  private void setupVideoDemoUiComponents() {
    // The Intent to access gallery and read a video file.
    videoGetter =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              Intent resultIntent = result.getData();
              if (resultIntent != null) {
                if (result.getResultCode() == RESULT_OK) {
                  glSurfaceView.post(
                      () ->
                          videoInput.start(
                              this,
                              resultIntent.getData(),
                              hands.getGlContext(),
                              glSurfaceView.getWidth(),
                              glSurfaceView.getHeight()));
                }
              }
            });
//    Button loadVideoButton = findViewById(R.id.button_load_video);
//    loadVideoButton.setOnClickListener(
//        v -> {
//          stopCurrentPipeline();
//          setupStreamingModePipeline(InputSource.VIDEO);
//          // Reads video from gallery.
//          Intent pickVideoIntent = new Intent(Intent.ACTION_PICK);
//          pickVideoIntent.setDataAndType(MediaStore.Video.Media.INTERNAL_CONTENT_URI, "video/*");
//          videoGetter.launch(pickVideoIntent);
//        });
  }

  /** Sets up the UI components for the live demo with camera input. */
//  private void setupFlutterScreenInit() {
//    Button startCameraButton = findViewById(R.id.flutter_screen);
//
//    startCameraButton.setOnClickListener(
//      v -> {
//        startActivity(
//          FlutterActivity
//            .withCachedEngine("my_engine_id")
//            .build(getApplicationContext())
//        );
//      });
//  }


  /** Sets up the UI components for the live demo with camera input. */
  private void setupLiveDemoUiComponents() {
    startCameraButton.setOnClickListener(
      v -> {
        if (inputSource == InputSource.CAMERA) {
          return;
        }
        startCameraButton.setVisibility(View.GONE);
        captureImageButton.setVisibility(View.VISIBLE);
        stopCameraButton.setVisibility(View.VISIBLE);
        setupStreamingModePipeline(InputSource.CAMERA);
      });
  }

  private void stopLiveDemoUiComponent() {
      stopCameraButton.setOnClickListener(
              v -> {
                stopCamera();
                stopCameraButton.setVisibility(View.GONE);
              });
  }

  public void showConfirmationDialog() {
    LayoutInflater li = LayoutInflater.from(context);
    View promptsView = li.inflate(R.layout.dialog, null);

    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
    alertDialogBuilder.setView(promptsView);

    final EditText uniqueId = promptsView.findViewById(R.id.uniqueId);
    final EditText setNo = promptsView.findViewById(R.id.setNo);

    alertDialogBuilder
            .setTitle("Confirm file details")
            .setMessage("Following details are used for file naming")
            .setCancelable(false)
            .setPositiveButton("Save",
                    (dialog, id) -> {
                      System.out.println("==== uniqueId: "+uniqueId.getText()
                              +", setNo: "+setNo.getText());

                      new android.os.Handler(Looper.getMainLooper()).postDelayed(
                        () -> {
                          try {
                            imageView.captureImage(getApplicationContext(),
                                    uniqueId.getText().toString(), setNo.getText().toString());
                          } catch (IOException e) {
                            System.out.println("===== CAUGHT EXCEPTION IMAGE CAP: " + e.getMessage());
                            e.printStackTrace();
                          }
//                          stopCamera();
                          System.out.println("====== STOP :: CAPTURE IMAGE ===== ");
                        }, 1000);
                    })
            .setNegativeButton("Cancel",
                    (dialog, id) -> dialog.cancel());

    AlertDialog alertDialog = alertDialogBuilder.create();
    alertDialog.show();
  }

  /** Capture image */
  private void setupCaptureImageUiComponents() {
    if(captureImageButton != null) {
      captureImageButton.setOnClickListener(
              v -> {
                stopCamera();
                System.out.println("====== START :: CAPTURE IMAGE =====");
                showConfirmationDialog();
//                new android.os.Handler(Looper.getMainLooper()).postDelayed(
//                        () -> { },
//                        1000);
              }
      );
    }
  }

  /** Sets up core workflow for streaming mode. */
  private void setupStreamingModePipeline(InputSource inputSource) {
    this.inputSource = inputSource;
    // Initializes a new MediaPipe Hands solution instance in the streaming mode.
    hands =
        new Hands(
            this,
            HandsOptions.builder()
                .setStaticImageMode(false)
                .setMaxNumHands(2)
                .setRunOnGpu(RUN_ON_GPU)
                .build());
    hands.setErrorListener((message, e) -> Log.e(TAG, "Kwik Capture error:" + message));

    if (inputSource == InputSource.CAMERA) {
      cameraInput = new KCCameraInput(this);
      cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
    } else if (inputSource == InputSource.VIDEO) {
      videoInput = new VideoInput(this);
      videoInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
    }

    // Initializes a new Gl surface view with a user-defined HandsResultGlRenderer.
    glSurfaceView =
        new SolutionGlSurfaceView<>(this, hands.getGlContext(), hands.getGlMajorVersion());
    glSurfaceView.setSolutionResultRenderer(new HandsResultGlRenderer());
    glSurfaceView.setRenderInputImage(true);
    hands.setResultListener(
        handsResult -> {
//          logIndexFingerTipLandmark(handsResult);

          imageView.setHandsResult(handsResult);
          runOnUiThread(() -> imageView.update());

          glSurfaceView.setRenderData(handsResult);
          glSurfaceView.requestRender();
        });

    // The runnable to start camera after the gl surface view is attached.
    // For video input source, videoInput.start() will be called when the video uri is available.
    if (inputSource == InputSource.CAMERA) {
      glSurfaceView.post(this::startCamera);
    }

    // Updates the preview layout.
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    imageView.setVisibility(View.GONE);
    frameLayout.removeAllViewsInLayout();
    frameLayout.addView(glSurfaceView);
    glSurfaceView.setVisibility(View.VISIBLE);
    frameLayout.requestLayout();
  }

  private void startCamera() {
    cameraInput.start(
        this,
        hands.getGlContext(),
        KCCameraInput.CameraFacing.BACK,
        glSurfaceView.getWidth(),
        glSurfaceView.getHeight());
  }

  private void stopCamera() {
    captureImageButton.setVisibility(View.GONE);
    stopCameraButton.setVisibility(View.GONE);
    startCameraButton.setVisibility(View.VISIBLE);
    if(inputSource == InputSource.CAMERA) {
      inputSource = InputSource.UNKNOWN;
    }
    if (cameraInput != null) {
      cameraInput.setNewFrameListener(null);
      cameraInput.close();
    }
    if (videoInput != null) {
      videoInput.setNewFrameListener(null);
      videoInput.close();
    }
    if (glSurfaceView != null) {
      glSurfaceView.setVisibility(View.GONE);
    }
    if (hands != null) {
      hands.close();
    }
  }

  private void stopCurrentPipeline() {
    if (cameraInput != null) {
      cameraInput.setNewFrameListener(null);
      cameraInput.close();
    }
    if (videoInput != null) {
      videoInput.setNewFrameListener(null);
      videoInput.close();
    }
    if (glSurfaceView != null) {
      glSurfaceView.setVisibility(View.GONE);
    }
    if (hands != null) {
      hands.close();
    }
  }

  private void logIndexFingerTipLandmark(HandsResult result) {
    if (result.multiHandLandmarks().isEmpty()) {
      return;
    }
    NormalizedLandmark indexFingerTipLandmark =
        result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.INDEX_FINGER_TIP);

//    ==== coordinates (in pixels): x=129.899048, y=410.691040
//    ==== normalized coordinates (value range: [0, 1]): x=0.169139, y=0.401065
//    ==== world coordinates (in meters): x=-0.027425 m, y=-0.077076 m, z=-0.048086 m

      int width = result.inputBitmap().getWidth();
      int height = result.inputBitmap().getHeight();
      Log.i(TAG, String.format("==== coordinates (in pixels): x=%f, y=%f",
              indexFingerTipLandmark.getX() * width, indexFingerTipLandmark.getY() * height));
      Log.i(TAG, String.format("==== normalized coordinates (value range: [0, 1]): x=%f, y=%f",
              indexFingerTipLandmark.getX(), indexFingerTipLandmark.getY()));

    if (result.multiHandWorldLandmarks().isEmpty()) {
      return;
    }
    Landmark indexFingerTipWorldLandmark =
        result.multiHandWorldLandmarks().get(0).getLandmarkList().get(HandLandmark.INDEX_FINGER_TIP);
    // (in meters with the origin at the hand's approximate geometric center)
    Log.i(
        TAG,
        String.format(
            "==== world coordinates (in meters): x=%f m, y=%f m, z=%f m",
                indexFingerTipWorldLandmark.getX(), indexFingerTipWorldLandmark.getY(),
                indexFingerTipWorldLandmark.getZ()));

//    NormalizedLandmark fifthLandmark =
//            result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.INDEX_FINGER_MCP);
//    NormalizedLandmark seventeenthLandmark =
//            result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.PINKY_MCP);
//
//    double distance = Math.sqrt(
//            Math.pow(((seventeenthLandmark.getY() * height) - (fifthLandmark.getY() * height)), 2)
//                    + Math.pow(((seventeenthLandmark.getX() * width) - (fifthLandmark.getX() * width)), 2));

//    runOnUiThread(() -> {
//      Toast mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
//
//      if (distance < 380.0) {
//        if(mToast!=null) {
//          mToast.setText("Bring closer");
//          mToast.show();
//        }
//      } else if (distance > 450.0) {
//        if(mToast!=null) {
//          mToast.setText("Move back");
//          mToast.show();
//        }
//      } else {
//        if(mToast!=null) {
//          mToast.cancel();
//        }
//      }
//    });
//    Log.i(TAG, "\n======== distance: "+distance+" =========\n");

  }
}