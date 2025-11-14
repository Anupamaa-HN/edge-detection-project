#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"native-lib",__VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_com_example_edgeviewer_MainActivity_nativeInit(JNIEnv* env, jobject) {
    LOGI("nativeInit");
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_edgeviewer_MainActivity_nativeProcessFrame(JNIEnv* env, jobject,
                                                           jbyteArray nv21_, jint width, jint height) {
    jbyte* nv21 = env->GetByteArrayElements(nv21_, NULL);
    cv::Mat yuv(height + height/2, width, CV_8UC1, (unsigned char*)nv21);
    cv::Mat bgr;
    cv::cvtColor(yuv, bgr, cv::COLOR_YUV2BGR_NV21);
    cv::Mat gray;
    cv::cvtColor(bgr, gray, cv::COLOR_BGR2GRAY);
    cv::Mat edges;
    cv::Canny(gray, edges, 80, 180);
    cv::Mat rgba;
    cv::cvtColor(edges, rgba, cv::COLOR_GRAY2RGBA);
    int outSize = rgba.total() * rgba.elemSize();
    jbyteArray out = env->NewByteArray(outSize);
    env->SetByteArrayRegion(out, 0, outSize, reinterpret_cast<jbyte*>(rgba.data));
    env->ReleaseByteArrayElements(nv21_, nv21, 0);
    return out;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_edgeviewer_MainActivity_nativeRelease(JNIEnv* env, jobject) {
    LOGI("nativeRelease");
}
