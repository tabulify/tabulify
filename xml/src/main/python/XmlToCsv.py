import xml.etree.ElementTree as ET

# This script parses XML and returns a CSV (tabulated TSV)
# Parameters
# fileInputPath = the path to the input file
# fileInputStructure = the structure of the input file (one xml by line or in xml by file)
# fileOutputPath = the path to the output file
#
# ns define a map between a namespace alias and the fuilly qualified namespace
# recordXPaths is a list of all xpath where the record begin. If we don;t found an element with the first xpath we try
#              the second one
# 
# Python Version 3.5

# Parameters
# XML Namespace are used in the Xpath to search the record node
ns = {'soap': 'http://schemas.xmlsoap.org/soap/envelope/',
      'wsproxy':'http://xml.inetpsa.com/Services/ApplicationPartenaire/WsProxy',
      'internet':'http://xml.inetpsa.com/Commerce/Client/Internet','tempuri':'http://tempuri.org/'}
recordXPaths = ["./soap:Body/wsproxy:sendClient/","./soap:Body/tempuri:RDVCitroenStore/"]
# fileInputPath = "../resources/SoapXmls.txt"
# fileOutputPath = "output.tsv"
# fileStructure="OneXmlByLine"
fileInputPath = "../resources/SoapSpec.xml"
fileOutputPath = "SoapSpec.tsv"
fileInputStructure= "OneXml"

# tag return also the namespace and there is no function to of only the name
# See https://bugs.python.org/issue18304
def get_tag_name(tag):
    return tag.split('}', 1)[1]

# Print a tree
def print_tree(tag):
    for child in tag:
        print(child)
        print_tree(child)


columnNameToIndexMap = {}; # Data Set Structure, columnIndex <-> columnName
columnIndexToNameMap = {}; # Data Set Structure, columnName <-> columnIndex
recordsMap = [] # Contains all records


def add_column_value_to_record(column_name,column_value):
    column_index = columnNameToIndexMap.get(column_name)
    if column_index is None:
        column_index = len(columnNameToIndexMap) + 1;
        columnNameToIndexMap[column_name] = column_index;
        columnIndexToNameMap[column_index] = column_name;
    recordMap[column_name] = column_value


lineCounter = 0
parseError = 0
noElementFound = 0

fileInputStream = open(fileInputPath)
if fileInputStructure == "OneXmlByLine":
    inputSoapXmls = fileInputStream
else:
    inputSoapXmls = [fileInputStream.read()]

for inputSoapXml in inputSoapXmls:

    lineCounter += 1

    # tree = ET.parse('../resources/SoapXml.xml')
    # root = tree.getroot()
    try:
        root = ET.fromstring(inputSoapXml)
    except:
        # print('Parsing Error, continue')
        parseError += 1
        continue

    # print('Print Tree')
    # print(root)
    # print_tree(root)

    # sendClient node
    # print()
    # print("sendClient Child's")
    for recordXPath in recordXPaths:
        sendClient = root.findall(recordXPath, ns)
        if len(sendClient) != 0:
            break;
    if len(sendClient) == 0:
        noElementFound += 1;
        continue;
    recordMap = {}
    add_column_value_to_record('rownum', lineCounter)
    for sendClientChild in sendClient:
        sendClientChildTagName = get_tag_name(sendClientChild.tag)
        # print(get_tag_name(sendClientChild.tag))
        for leafChild in sendClientChild:
            columnName = (sendClientChildTagName + '.' + get_tag_name(leafChild.tag)).lower()
            # print('   ',columnName, ' : ', leafChild.text)
            add_column_value_to_record(columnName,leafChild.text)
    # print('Add record')
    recordsMap.append(recordMap)

fileInputStream.close()
print('Number of Records successfully parsed:',len(recordsMap))
print('Number of Records with parsing error:',parseError)
print('Number of Records where no element where found:',noElementFound)
print('-------------------------------------------------------')
print('Number of Lines:', lineCounter)
print()
if lineCounter != len(recordsMap) + parseError + noElementFound:
    print('Warning: the number of line doesn''t sum up to the number of records + the number of parse error +  the number of element not found')

outputFile = open(fileOutputPath,'w+')
# Print the headers
for index in columnIndexToNameMap.keys():
    # print(columnIndexToNameMap[index], end='\t')
    outputFile.write(columnIndexToNameMap[index]+'\t')
# print()
outputFile.write('\n')
## Print the line
for recordMap in recordsMap:
    for columnIndex, columnName in iter(columnIndexToNameMap.items()):
        try:
            # print(recordMap[columnName],end='\t')
            if recordMap[columnName] is not None:
                outputFile.write(str(recordMap[columnName])+'\t')
            else:
                outputFile.write('\t')
        except KeyError:
            # print(end='\t')
            outputFile.write('\t')
    # print()
    outputFile.write('\n')

outputFile.close()
print('The output file was generated')




