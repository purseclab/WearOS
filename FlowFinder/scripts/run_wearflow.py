import os
import sys
import subprocess
import time

print("Running wearflow script")

directory = ''
if (len(sys.argv)==2):
  directory = sys.argv[1]
else:
    print("Error, please input the directory")
    sys.exit()


wearflow_dir = '/media/frost/hardWolf/PurSec/WearOS/wearflowDebug/Wearflow-Fixes/'
platform = '/home/frost/Android/Sdk/platforms'


apks = [name for name in os.listdir(directory) if name.endswith(".apk")]
if (len(apks) == 2):
    result_paths = ""
    matching_phase = True
    for apk in apks:
        try:
            apk_path = directory + "/"+ apk

            subprocess.call(['java', '-Xmx8g', '-jar',
                             wearflow_dir + 'target/wearflow-cmd-jar-with-dependencies.jar',
                              '-s', wearflow_dir + 'SourcesAndSinks.txt',
                              '-p', platform ,'-a', apk_path, '-wsc',
                              wearflow_dir + 'config-string-analysis.txt',
                              '-tw', wearflow_dir + 'EasyTaintWrapperSource.txt'])    #

            output = apk_path.replace(".apk",".json")
            if os.path.exists(output):
                result_paths = result_paths + output + ";"
            else:
                matching_phase = False;
                print("WearFlow finish, nothing to match")
                break;

        except Exception as e:
            print(e)

    if matching_phase == True:
        print("Flow Analysis finished, starting Matching step")
        result_paths = result_paths[:-1]
        subprocess.call(['java', '-jar', wearflow_dir + 'target/wearflow-cmd-jar-with-dependencies.jar',
         '-wfa', '-imatch', result_paths])

