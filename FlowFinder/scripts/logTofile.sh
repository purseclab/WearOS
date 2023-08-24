java -Xmx16g -jar /media/frost/hardWolf/PurSec/WearOS/wearflowDebug/Wearflow-Fixes/target/wearflow-cmd-jar-with-dependencies.jar \
-s /media/frost/hardWolf/PurSec/WearOS/wearflowDebug/Wearflow-Fixes/negativeSinks.txt \
-p /home/frost/Android/Sdk/platforms \
-a /media/frost/hardWolf/PurSec/WearOS/wearApks/Negatives/com.sparkistic.photowearcode/phone.apk \
-wsc /media/frost/hardWolf/PurSec/WearOS/wearflowDebug/Wearflow-Fixes/config-string-analysis.txt \
-tw /media/frost/hardWolf/PurSec/WearOS/wearflowDebug/Wearflow-Fixes/EasyTaintWrapperSource.txt > ../log.txt 2>&1

# /media/frost/hardWolf/PurSec/WearOS/wearApks/Negatives/com.thehoodiestudio.rwrk/phone.apk 
# /media/frost/hardWolf/PurSec/WearOS/wearApks/TaintTest/app-debug.apk
# /media/frost/hardWolf/PurSec/WearOS/wearApks/com.moletag.gallery/phone_base.apk
# /media/frost/hardWolf/PurSec/WearOS/wearApks/com.skimble.workouts/wear.apk
# /media/frost/hardWolf/PurSec/WearOS/wearApks/com.spiraledge.swimapp/wear_base.apk
# /media/frost/hardWolf/PurSec/WearOS/wearApks/com.coffeebeanventures.easyvoicerecorder/wear_base.apk
# /media/frost/hardWolf/PurSec/WearOS/wearApks/com.estela/phone_base.apk
# SourcesAndSinks jsonSrcSink.txt