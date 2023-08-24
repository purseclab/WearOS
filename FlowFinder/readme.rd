# wearflow
Static Analysis tool to compute sensitive data flows across mobile and companion Android apps.

# Getting the tool

Wearflow uses a modified version of Flowdroid to support the analysis of wearable apps.
This version includes a signature-based deobfuscator of wearable APIs, modelling and instrumentation
of such APIs and string analysis using Violist.
https://github.com/secure-software-engineering/FlowDroid
https://github.com/USC-SQL/Violist


# Requierements:

1 - clone this repo
2 - java jdk 7/8, maven
3 - Android Jars from the Android SDK
4 - SourcesAndSinks.txt, AndroidCallbacks.txt, and EasyTaintWrapperSource.txt files from this repository.
5 - Mobile and wearable APK.

# How to use Wearflow

You can either run directly the file wearflow-cmd-jar-with-dependencies.jar or build WearFlow with maven.

To build WearFlow:

1- clone this repo

2 - Go to the custom FlowDroid directory and run the maven install command

path/to/wearflow/FlowDroid> mvn clean install

3- Go to WearFlow directory and install with maven

path/to/wearflow> mvn clean install



# Running the tool

- The RECOMMENDED option is to run the script run_wearflow.py located in the scripts directory.

1- Modify the wearflow-path and the platform-path in the script.

The wearflow-path is the main directory of this repository. e.g ~/git-repos/wearflow
The platform-path is the path to the Android Jars files. Usually located at $ANDROID_HOME/platforms

2- run the run_wearflow.py script:

path/to/wearflow/scripts>python3 run_wearflow.py <path_to_APKs>

<path_to_APKS> is the directory where the mobile and wearable APKs are located.
If you only have the mobile APK, you can use the extract_wear_apk.py script
to extract the wearable APK from the mobile APK.

- Alternatively, you can run wearflow directly. In this case, you need to run WearFlow
individually foreach APK with the following parameters:

path/to/wearflow/target> java -jar wearflow-cmd-jar-with-dependencies.jar \\
-p path/to/platforms \\
-a path/to/apk \\
-s path/to/SourcesAndSinks.txt \\
-wsc path/to/config-string-analysis.txt \\
-tw  path/to/EasyTaintWrapperSource.txt

The output will be written in the <path_to_APKS> directory. This is a json file
with the same name as the APK

Then, to run the matching analysis, you'll need to run werflow with the
following options:

path/to/wearflow/target> java -jar wearflow-cmd-jar-with-dependencies.jar \\
-wfa \\
-imatch <path_to_APKs/mobile_output.json>;<path_to_APKs/wear_output.json>
