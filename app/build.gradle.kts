plugins {
	alias(libs.plugins.android.application)
}

android {
	namespace = "com.example.tsuyu"
	compileSdk {
		version = release(37)
	}

	defaultConfig {
		applicationId = "com.example.tsuyu"
		minSdk = 24
		targetSdk = 37
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			optimization {
				enable = true
				packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
			}
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	packaging {
		jniLibs {
			useLegacyPackaging = false
		}
	}
}

dependencies {
	implementation(libs.androidx.activity.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.core.ktx)
	implementation(libs.material)
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
	implementation("com.microsoft.onnxruntime:onnxruntime-android:1.30.0")
	implementation("com.vanniktech:android-image-cropper:4.5.0")
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(libs.androidx.junit)
}