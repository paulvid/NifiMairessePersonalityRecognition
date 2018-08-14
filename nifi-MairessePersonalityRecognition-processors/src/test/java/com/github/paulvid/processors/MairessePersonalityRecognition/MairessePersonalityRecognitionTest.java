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
        testRunner.setProperty(MairessePersonalityRecognition.MODELS_BASE_DIR,"/Users/pvidal/Documents/nifi_precessors/mairesse-personality-recognition/to-upload-to-nifi-lib-folder/models/");
        testRunner.setProperty(MairessePersonalityRecognition.ARFF_PATH,"/Users/pvidal/Documents/nifi_precessors/mairesse-personality-recognition/to-upload-to-nifi-lib-folder/attributes-info.arff");
        testRunner.setProperty(MairessePersonalityRecognition.LIWC_PATH,"/Users/pvidal/Documents/nifi_precessors/mairesse-personality-recognition/to-upload-to-nifi-lib-folder/LIWC.CAT");
        testRunner.setProperty(MairessePersonalityRecognition.MRC_PATH,"/Users/pvidal/Documents/nifi_precessors/mairesse-personality-recognition/to-upload-to-nifi-lib-folder/mrc2.dct");
        testRunner.setProperty(MairessePersonalityRecognition.OUTPUT_COUNT_FLAG,"No");
        testRunner.setProperty(MairessePersonalityRecognition.OUTPUT_MODEL_FLAG,"No");
        testRunner.setProperty(MairessePersonalityRecognition.INPUT_TYPE,"Written Language");
        testRunner.setProperty(MairessePersonalityRecognition.MODEL_TYPE,"M5 Regression Tree");

        testRunner.enqueue("Iraqi Leader Offers to Help Family of Spy Who Infiltrated ISIS\n" +
                "Image\n" +
                "Capt. Harith al-Sudani’s wife, Raghad Hasan Chaloob, and three children, at home in Baghdad.CreditIvor Prickett for The New York Times\n" +
                "By Margaret Coker\n" +
                "Aug. 14, 2018\n" +
                "BAGHDAD — The father of the Iraqi spy who spent 16 months undercover inside the Islamic State said Tuesday that his family’s long struggle to receive his son’s pension and death benefits has finally borne some fruit.\n" +
                "Iraqis have been abuzz with the story of Capt. Harith al-Sudani, the man Iraqi officials describe as the country’s most successful spy, since The New York Times published a story on Sunday about him and his secret intelligence unit called Suquor, or the Falcons.\n" +
                "As a mole inside the terrorist group, Captain Sudani foiled dozens of planned attacks in the Iraqi capital and provided his Iraqi commanders and the American-led coalition fighting the group a live wire into the senior ranks of the organization. The Islamic State discovered Captain Sudani’s real identity and killed him in August 2017, according to Iraqi intelligence officials.\n" +
                "The Sudani family has been trying for a year to get recognition for Captain Sudani’s sacrifice and his pension for his widow and three children, only to be stymied by Iraq’s infamous bureaucracy, according his father, Abid al-Sudani.\n" +
                "Captain Sudani was killed by the Islamic State behind enemy lines and his family does not have a body to produce as proof of death, despite statements last year by his intelligence unit and Iraq’s joint military command announcing his death as part of the wider operations to defeat the terror group.\n" +
                "During the last two days, all major Iraqi television newscasts and several Pan-Arab news channels have run features about the Falcons and Captain Sudani’s derring-do, based on the Times story. His heroism has been the talk in tea shops around Baghdad, Iraqi social media sites and among passengers waiting in passport control at the airport.\n" +
                "Amid the media buzz, an assistant to Prime Minister Haider al-Abadi called on the Sudani family on Monday evening at their home in the eastern Baghdad neighborhood of Sadr City and offered to intervene on their behalf, according to Mr. Sudani.\n" +
                "During an hourlong visit, the official took notes on Captain Sudani’s case and called the appropriate administrative court judges who could issue a formal death certificate, Mr. Sudani said.\n" +
                "Among the family, the attention from the government prompted gratitude, tinged with a bit of bitterness.\n" +
                "“He didn’t apologize for taking so long to pay attention to our suffering. But I didn’t point this out to him,” Mr. Sudani said of the government official who visited the family. “I know he was sincere in trying to help us now. That is the important thing.”\n" +
                "A separate official from Mr. Abadi’s office said that families of dozens of fallen Iraqi servicemen, in addition to the Sudani family, have also had trouble overcoming bureaucratic hurdles to receive death benefits for those killed during the operations again the Islamic State. The official confirmed the visit to the Sudani family.\n" +
                "The Falcons are among the most important counterterrorism services aiding the Americans in the fight against extremism. The unit was involved in a recent Iraqi-American sting based on Iraqi intelligence that led to the arrest of five senior Islamic State members who had been hiding in Turkey and Syria. Iraqi officials say the Falcons have foiled hundreds of attacks on Baghdad, making the capital the safest it has been in 15 years.\n" +
                "American military officials consider the agency as good as they get among non-Western spy services.\n" +
                "“It has proved to be an extremely valuable unit,” said Col. Sean J. Ryan, a spokesman for the American-led military coalition in Baghdad. The Falcons, he said, have diminished the threat of the Islamic State by infiltrating its cells, killing its leaders and terrorists, and destroying its weapons.\n" +
                "Falih Hassan\n");
        testRunner.run();
        //List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(ExecuteSqoop.SUCCESS);
        //assertEquals(1, flowFiles.size());
        //MockFlowFile flowFile = flowFiles.get(0);
        //assertEquals("randombytes-1", flowFile.getAttribute(CoreAttributes.FILENAME.key()));
        //assertEquals("target/test-classes", flowFile.getAttribute(PutHDFS.ABSOLUTE_HDFS_PATH_ATTRIBUTE));
    }

}
