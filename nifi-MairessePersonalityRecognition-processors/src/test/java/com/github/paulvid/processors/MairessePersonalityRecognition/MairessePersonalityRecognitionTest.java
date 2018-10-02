/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.paulvid.processors.MairessePersonalityRecognition;

import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;


public class MairessePersonalityRecognitionTest {

    private TestRunner testRunner;

    @Before
    public void init() {
        testRunner = TestRunners.newTestRunner(MairessePersonalityRecognition.class);
    }

    @Test
    public void testProcessor() {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(MairessePersonalityRecognition.MODELS_BASE_DIR,"/Users/pvidal/Documents/nifi_precessors/mairesse-personality-recognition/to-upload-to-nifi-lib-folder/models");
        testRunner.setProperty(MairessePersonalityRecognition.ARFF_PATH,"/Users/pvidal/Documents/nifi_precessors/mairesse-personality-recognition/to-upload-to-nifi-lib-folder/attributes-info.arff");
        testRunner.setProperty(MairessePersonalityRecognition.LIWC_PATH,"/Users/pvidal/Documents/nifi_precessors/mairesse-personality-recognition/to-upload-to-nifi-lib-folder/LIWC.CAT");
        testRunner.setProperty(MairessePersonalityRecognition.MRC_PATH,"/Users/pvidal/Documents/nifi_precessors/mairesse-personality-recognition/to-upload-to-nifi-lib-folder/mrc2.dct");
        testRunner.setProperty(MairessePersonalityRecognition.OUTPUT_COUNT_FLAG,"No");
        testRunner.setProperty(MairessePersonalityRecognition.OUTPUT_MODEL_FLAG,"No");
        testRunner.setProperty(MairessePersonalityRecognition.INPUT_TYPE,"Written Language");
        testRunner.setProperty(MairessePersonalityRecognition.MODEL_TYPE,"M5 Model Tree");

        testRunner.enqueue("Apple Wins Reversal in Univ. Of Wisconsin Patent Case " +
                "By Reuters " +
                "Sept. 28, 2018 " +
                "(Reuters) - Apple Inc persuaded a federal appeals court on Friday to throw out a $234 million damages award in favor of the University of Wisconsin's patent licensing arm for infringing the school's patent on computer processing technology. " +
                "The U.S. Federal Circuit Court of Appeals in Washington, D.C. said no reasonable juror could have found infringement based on evidence presented during the liability phase of the 2015 trial. " +
                "It said Apple deserved judgment as a matter of law in the case brought by the Wisconsin Alumni Research Foundation. " +
                "(Reporting by Jonathan Stempel in New York)");
        testRunner.run();

        //List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(ExecuteSqoop.SUCCESS);
        //assertEquals(1, flowFiles.size());
        //MockFlowFile flowFile = flowFiles.get(0);
        //assertEquals("randombytes-1", flowFile.getAttribute(CoreAttributes.FILENAME.key()));
        //assertEquals("target/test-classes", flowFile.getAttribute(PutHDFS.ABSOLUTE_HDFS_PATH_ATTRIBUTE));
    }

}
