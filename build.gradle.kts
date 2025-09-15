// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.8.0-alpha05" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Google services plugin for Firebase
        classpath("com.google.gms:google-services:4.3.15")
    }
}

repositories {
    google()
    mavenCentral()
}