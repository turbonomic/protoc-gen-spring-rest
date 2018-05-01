#!/usr/bin/python

import shutil
import argparse
import os

pom_template = '''<?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <parent>
            <groupId>com.vmturbo</groupId>
            <artifactId>protoc-gen-spring-rest-test</artifactId>
            <version>HEAD-SNAPSHOT</version>
            <relativePath>../pom.xml</relativePath>
        </parent>
        <modelVersion>4.0.0</modelVersion>

        <artifactId>{}</artifactId>

        <properties>
          <checkstyle.path>${{project.basedir}}/../../../build/checkstyle.xml</checkstyle.path>
        </properties>
    </project>
'''

proto_template = '''syntax = "proto2";
package com.vmturbo.protoc.spring.rest.{};
'''

def main():
   parser = argparse.ArgumentParser(description='Generate a test for the protoc-spring-rest protobuf compiler plugin.')
   parser.add_argument('name', help='The name of the test. The program will add a \'test\' prefix')
   args = parser.parse_args()
   capitalized_name = args.name[:1].upper() + args.name[1:]
   test_name = "test" + capitalized_name
   proto_name = capitalized_name + "Test.proto"

   test_dir = os.path.join(os.getcwd(), test_name)
   protobuf_dir = os.path.join(test_dir, "src", "test", "protobuf")
   pom_path = os.path.join(test_dir, "pom.xml")
   proto_path = os.path.join(protobuf_dir, proto_name)

   print "Creating folder: " + test_dir
   os.mkdir(test_dir)

   try:
      print "Creating folder: " + protobuf_dir
      os.makedirs(protobuf_dir)
      print "Writing file: " + proto_path
      with open(proto_path, 'w') as proto_file:
         proto_file.write(proto_template.format(test_name))

      print "Writing file: " + pom_path
      with open(pom_path, 'w') as pom_file:
         pom_file.write(pom_template.format(test_name))
   except Exception as e:
      print "Failed due to exception: " + str(e)
      print "Removing folder: " + test_dir
      shutil.rmtree(test_dir)
      
   # testName
   # testName/pom.xml
   # testName/src/test/protobuf
   # testName/src/test/protobuf/NameTest.proto

if __name__ == "__main__":
   main()
