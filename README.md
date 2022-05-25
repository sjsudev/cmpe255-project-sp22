# cmpe255-project-sp22

Prediction of Kitchen Layout and Charecteristics using Mobile Camera.

### Problem Statement : 

       a)Kitchen Remodeling - Customers don't have a way to find suitable products fit for their needs and kitchens. No application available to provide real-time estimates of remodeling costs. 
       b)Physical Quotes - Remodeling requires contractors to visit your home and estimate the costs. Also no DIY or pandemic friendly options available.

### Existing Difficulties : 

       There aren't enough open-source datasets to train a classification model for kitchens. To predict meaningful classifiers with high accuracy, a large dataset is required. For a real-time solution, low latency is also required. 

       This project has successfully achieved the required dataset, low response time, and applications.
       
### Software Tools Used :
   a) Python Flask
   b) Tensorflow / Tensorflow Lite
   c) Android / MLKit
   d) AWS SageMaker, S3, EC2

### Contribution:
1. Devansh Modi - Modeling, Datasets, Python Flask, and Inference API
2. Abhighna Kapivalai - Android App, Search API, Research

### RESTful Inference API

This project uses a Flask Python based API to allow inference requests for the multiple models.

Flask app can be started by the following command.

`python flask run`

Request 1 - Get Predictions (without Acceleration)
```
curl --location --request POST 'http://ec2-35-165-5-106.us-west-2.compute.amazonaws.com:5000/predict' \
--form 'image1=@"/path/to/file"'
```

Request 1 - Get Predictions (with Tensorflow Acceleration)
```
curl --location --request POST 'http://ec2-35-161-1-34.us-west-2.compute.amazonaws.com:5000/predict' \
--form 'image1=@"/path/to/file"'
```

### Android App

The android application uses both API calls and Tensorflow lite model to perform inference.

Here is an example of how the app accomplishes local inference.

```
Interpreter interpreter = new Interpreter(FileUtil.loadMappedFile(this, "converted_model.tflite"));

// Creates inputs for reference.
TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 512, 512, 3}, DataType.FLOAT32);
Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(imageFile.getPath()), 512, 512, true);
ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);
interpreter.run(byteBuffer, result);

```

### GPU Acceleration

Android can use Tensorflow GPU acceleration with the following steps.

1. Add the following module to build.gradle

`implementation 'org.tensorflow:tensorflow-lite-gpu:2.3.0'`

2. Update Tensorflow code in Android Upload.java class

```
Interpreter.Options options = new Interpreter.Options();
CompatibilityList compatList = new CompatibilityList();

if(compatList.isDelegateSupportedOnThisDevice()){
       // if the device has a supported GPU, add the GPU delegate
       GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
       GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
       options.addDelegate(gpuDelegate);
} else {
       // if the GPU is not supported, run on 4 threads
       options.setNumThreads(4);
}
```