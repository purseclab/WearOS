import os
import xml.etree.ElementTree as ET

summaryPath = "/media/frost/hardWolf/PurSec/WearOS/wearflowDebug/Wearflow-Fixes/FlowDroid/soot-infoflow-summaries/summariesManual/"

if not os.path.exists(summaryPath):
    print("Summary folder does not exist. Give path to FlowDroid/soot-infoflow-summaries/summariesManual/")
    exit()

xmlsDir = os.listdir(summaryPath)

def processXML(summaryPath, xml):
    if xml.endswith(".xml"):
        tree  = ET.parse(summaryPath+xml)
        root = tree.getroot()
        if root.tag == "summary":
            for methods in root:
                if methods.tag == "methods":
                    for method in methods:
                        if method.tag == "method":
                            print("<"+xml[:-4]+": "+method.attrib['id']+">")

# read all xml files in the directory
for xml in xmlsDir:
    if xml.endswith(".xml"):
        processXML(summaryPath, xml)




# for flows in method:
#     if flows.tag == "flows":
#         for flow in flows:
#             if flow.tag == "flow":
#                 for source in flow:
#                     if source.tag == "source":
#                         for sink in flow:
#                             if sink.tag == "sink":
#                                 print(source.attrib)
#                                 print(sink.attrib)
#                                 print("")
