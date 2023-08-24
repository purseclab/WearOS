import os
import shutil
import sys

directory = ''
if (len(sys.argv)==2):
  directory = sys.argv[1]
else:
    print("Error, please input the directory")
    sys.exit()
     
try:
     
    files = [name for name in os.listdir(directory) if name.endswith(".apk")]
    if len(files) == 1:
        file_path = directory + '/' + files[0]
        zip_apk = file_path + '.zip'
        #renamed_app_path = directory + '/' + zip_apk
        # move the apk and add .zip extension
        shutil.move(file_path, zip_apk )
        # unzip the apk
        unzip_path = directory + "/unzip"
        unzip_command = "unzip -q " + zip_apk + " -d " + unzip_path
        os.system(unzip_command)

        # remove the .zip extension from the original apk
        shutil.move(zip_apk, zip_apk.replace('.zip',''))

        # move the wearable apk to the initial directory
        wear_path = unzip_path + "/res/raw"
        subdirs = os.listdir(wear_path)
        if len(subdirs) > 0:
            for tmp_file in subdirs:
                tfile = os.fsdecode(tmp_file)
                if tfile.endswith(".apk"):
                    apk_path = wear_path + "/" + tfile
                    new_apk_path = directory + "/" + tfile
                    shutil.move(apk_path, new_apk_path)
                    break
        
        shutil.rmtree(unzip_path)
                
except Exception as e:
    print(e)
