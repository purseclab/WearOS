import os
import sys
import subprocess
import time

directory = ''
if (len(sys.argv)==2):
    directory = sys.argv[1]
else:
    print("Error, please input the apk directory")
    sys.exit()

srcSinkDir = "/media/frost/hardWolf/PurSec/WearOS/doguData/frostSrcSinks/"
wearflow_dir = '/media/frost/hardWolf/PurSec/WearOS/Wearflow-Fixes/'
platform = '/home/frost/Android/Sdk/platforms'

apks = [name for name in os.listdir(directory) if name.endswith(".apk")]
if (len(apks) == 2):
    for apk in apks:
        apk_path = directory + apk
        customSrcSink = srcSinkDir + directory.split("/")[-2] + "/" + apk.replace(".apk",".txt")
        print("apk_path", apk_path)
        # print("customSrcSink", customSrcSink)
        try:
            subprocess.call(['java', '-Xmx8g', '-jar',
                        wearflow_dir + 'target/wearflow-cmd-jar-with-dependencies.jar',
                        '-s', customSrcSink,
                        '-p', platform ,'-a', apk_path, '-wsc',
                        wearflow_dir + 'config-string-analysis.txt',
                        '-tw', wearflow_dir + 'EasyTaintWrapperSource.txt'])    #
        except Exception as e:
            print(e)
else:
    print("Error, please input the apk directory")
    sys.exit()

