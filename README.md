# WearSunshine - Watch face for Android Wear

<img src="https://user-images.githubusercontent.com/15446842/32187439-fe126028-bd9c-11e7-9296-276f6c4a3d9e.png"/>

WearSunshine is wearable app companion for weather app [Sunshine](https://github.com/udacity/ud851-Sunshine) that runs on Android Wear devices. To tell more than time, the app takes advantage of Android Wear 2.0 API and the addition of customizable complications. It offers three complication slots to display additional information on the watch face, such as weather data provided by the mobile app.

**Watch Face Features:**
* Custom analog watch face that works on both round and square watch faces
* Offers range, short text and icon complications slots and by default shows:
  - High and low temperatures
  - Current weather icon
  - Humidity level
* Allows user to tap on complications launches mobile app
* Uses `Data Layer API` to synchronize data between devices
* Optimised for ambient and low bit ambient modes

**Mobile App Features:**
* Uses `ComplicationsProviderService` to expose Sunshine data
* Shows notifications on wearable device

## Try it out
To install the app on a connected device or running emulator, run:

```gradle
git clone https://github.com/mattwiduch/WearSunshine.git
cd WearSunshine
./gradlew installDebug
```

## License
```
Copyright (C) 2016 Mateusz Widuch

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
