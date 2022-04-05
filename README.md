# kwikCapture

AI Powered Smart Contactless Fingerprint Capturing Technology

![kwik capture logo](https://i.ibb.co/D892zyG/kc-logo.png)

## About kwikCapture

kwikCapture mobile app is an artificial intelligence (AI) powered smart contactless fingerprint capturing technology that utilizes built-in camera hardware alongside cross platform, customizable convolutional neural network model to detect human palm and fingers regardless of background type. Robust real-time hand perception is a decidedly challenging computer vision task, as hands often occlude themselves. kwikCapture mobile app is a high-fidelity hand and finger tracking solution. It employs machine learning (ML) to infer 21 3D landmarks of a hand from just a single frame.

## Technical Gaps Addressed & Improvements

kwikCapture app addressed following gaps: 

### Accurately Measuring Distance: 
kwikCapture uses hand landmarks to accurately measure the distance between the hand and a mobile phone by calculating a distance between the INDEX_FINGER_MCP and PINKY_MCP. As a hand gets closer to the camera, the distance increases and it decreases as a hand goes away from the camera. A real-time hand landmark detection has tremendously increased the accuracy of measuring the distance in this application and its an extremely lightweight solution as a stream of hand landmarks data is already available for the finger detection.

### Improve the Efficiency of Rendering Algorithms: 
kwikCapture utilizes OpenGL rendering algorithms specifically extended to work with ML models used in this project thus further improving the image rendering performance by default. Instead of relying too much on CPU, kwikCapture heavily relies on in-build GPU processing powers for image processing and running ML models for finger detection. GPU architecture allows parallel processing of image pixels which, in turn, leads to a reduction of the processing time for a single image in terms of latency.

### Free form contactless fingerprint collection: 
For portable capture devices, the motion of the device or the subject can negatively impact capture. The most challenging scenario encountered was a moving/unstable capture device coupled with a moving/unstable subject. Many contactless fingerprint capturing technologies in the market are somewhat restrictive when it comes to taking a high quality digital picture of a human hand. For instance, law enforcement officers might have to align their mobile device with a suspect’s hand to fit fingers in a predefined augmented box layout and then capture the image. Whereas kwikCapture provides freedom to capture the image as it implemented a hand tracking ML model to the tech stack of this app to overcome this issue.
    
## How to Run the App

Clone this repository on your machine and import it as an Android project in Android Studio IDE. Android studio should be able to recognize as an Android project. Now, click the "play" icon to run the project either on a physical connected device or on an emulator.

