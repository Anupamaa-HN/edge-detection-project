cmake_minimum_required(VERSION 3.10)
project("edgeviewer-native")

set(CMAKE_CXX_STANDARD 17)
add_library(native-lib SHARED native-lib.cpp)

# Update this to the location where you'll place OpenCV in the Codespace
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/../../../../OpenCV/sdk/native/jni)
find_package(OpenCV REQUIRED core imgproc)

include_directories(${OpenCV_INCLUDE_DIRS})
target_link_libraries(native-lib ${OpenCV_LIBS} log)
