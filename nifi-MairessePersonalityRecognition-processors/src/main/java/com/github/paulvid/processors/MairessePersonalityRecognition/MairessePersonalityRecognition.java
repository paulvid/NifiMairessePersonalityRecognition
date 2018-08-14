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

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import jmrc.EntryNotFoundException;
import jmrc.Field;
import jmrc.MRCDatabase;
import jmrc.MRCPoS;
import jmrc.PoS;
import jmrc.QueryException;
import jmrc.UndefinedValueException;


import org.apache.nifi.stream.io.StreamUtils;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.trees.M5P;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.experiment.Compute;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;
import weka.core.*;

@Tags({"ml, personality, detection, weka, liwc"})
@CapabilityDescription("Nifi Processor using Mairesse Personality Recognition library")
@WritesAttributes({
        @WritesAttribute(attribute="mairessepersonalityrecognition.extraversion", description="Extraversion personality trait evaluated score score from 1 to 7"),
        @WritesAttribute(attribute="mairessepersonalityrecognition.emotional_stability", description="Emotional stability (Neuroticism) personality trait evaluated score score from 1 to 7"),
        @WritesAttribute(attribute="mairessepersonalityrecognition.agreeableness", description="Agreeableness personality trait evaluated score score from 1 to 7"),
        @WritesAttribute(attribute="mairessepersonalityrecognition.conscientiousness", description="Conscientiousness personality trait evaluated score score from 1 to 7"),
        @WritesAttribute(attribute="mairessepersonalityrecognition.openness_to_experience", description="Openness to Experience personality trait evaluated score score from 1 to 7"),
        @WritesAttribute(attribute="mairessepersonalityrecognition.count_output", description="Feature count output (only if the property Output count flag is set to Yes)"),
        @WritesAttribute(attribute="mairessepersonalityrecognition.model_output", description="Model output (only if the property Output model flag is set to Yes)")
})
public class MairessePersonalityRecognition extends AbstractProcessor {

    // flowfile attribute keys returned after processing input
    public final static String EXTRAVERSION = "mairessepersonalityrecognition.extraversion";
    public final static String EMOTIONAL_STABILITY = "mairessepersonalityrecognition.emotional_stability";
    public final static String AGREEABLENESS = "mairessepersonalityrecognition.agreeableness";
    public final static String CONSCIENTIOUSNESS = "mairessepersonalityrecognition.conscientiousness";
    public final static String OPENNESS_TO_EXPERIENCE = "mairessepersonalityrecognition.openness_to_experience";
    public final static String COUNT_OUTPUT = "mairessepersonalityrecognition.count_output";
    public final static String MODEL_OUTPUT = "mairessepersonalityrecognition.model_output";


    // processor dropdown parameters
    public static final String WRITTEN_CONTENT = "Written Language";
    public static final String SPOKEN_CONTENT = "Spoken Language";
    public static final String LINEAR_REGRESSION = "Linear Regression";
    public static final String M5_MODEL_TREE = "M5 Model Tree";
    public static final String M5_REGRESSION_TREE = "M5 Regression Tree";
    public static final String SMOREG = "Support Vector Machine with Linear Kernel (SMOreg)";
    public static final String YES = "Yes";
    public static final String NO = "No";


    public static final PropertyDescriptor INPUT_TYPE = new PropertyDescriptor
            .Builder().name("INPUT_TYPE")
            .displayName("Input Type")
            .description("The appropriate model depends on the language sample (written or spoken), and whether observed personality (as perceived by external judges) or self-assessed personality (the writer/speaker's perception) needs to be estimated from the text. Options: Observed personality from spoken language or Self-assessed personality from written language")
            .required(true)
            .allowableValues(WRITTEN_CONTENT, SPOKEN_CONTENT)
            .defaultValue(WRITTEN_CONTENT)
            .build();

    public static final PropertyDescriptor MODEL_TYPE = new PropertyDescriptor
            .Builder().name("MODEL_TYPE")
            .displayName("Model Type")
            .description("Model to use for computing scores.")
            .required(true)
            .allowableValues(LINEAR_REGRESSION, M5_MODEL_TREE, M5_REGRESSION_TREE, SMOREG)
            .defaultValue(M5_REGRESSION_TREE)
            .build();

    public static final PropertyDescriptor OUTPUT_COUNT_FLAG = new PropertyDescriptor
            .Builder().name("OUTPUT_COUNT_FLAG")
            .displayName("Output count flag")
            .description("If set to Yes, processor will output feature count to attribute mairessepersonalityrecognition.model_output")
            .required(true)
            .allowableValues(YES, NO)
            .defaultValue(NO)
            .build();

    public static final PropertyDescriptor OUTPUT_MODEL_FLAG = new PropertyDescriptor
            .Builder().name("OUTPUT_MODEL_FLAG")
            .displayName("Output model flag")
            .description("If set to Yes, processor will output model to attribute mairessepersonalityrecognition.model_output")
            .required(true)
            .allowableValues(YES, NO)
            .defaultValue(NO)
            .build();

    public static final PropertyDescriptor MRC_PATH = new PropertyDescriptor
            .Builder().name("MRC_PATH")
            .displayName("MRC DB File Path")
            .description("File path to the MRC database (e.g. /home/nifi/perso-recognition/lib/mrc2.dct)")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor LIWC_PATH = new PropertyDescriptor
            .Builder().name("LIWC_PATH")
            .displayName("LIWC Dictionary File Path")
            .description("File path to the LIWC Dictionary (e.g. /home/nifi/perso-recognition/lib/LIWC.CAT)")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor ARFF_PATH = new PropertyDescriptor
            .Builder().name("ARFF_PATH")
            .displayName("Weka ARFF File Path")
            .description("File path to the Weka ARFF file with all attributes and no instance(e.g. /home/nifi/perso-recognition/lib/attributes-info.arff)")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MODELS_BASE_DIR = new PropertyDescriptor
            .Builder().name("MODELS_BASE_DIR")
            .displayName("Models Directory Path")
            .description("Directory path to the root of the different models (e.g. /home/nifi/perso-recognition/lib/models/)")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("A FlowFile is routed to this relationship after the text input in flowfile has been successfully analyzed")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("A FlowFile is routed to this relationship if an error occurred the analysis of the flowfile")
            .build();



    /**************************************/
    /** Mairesse Library specific params **/
    /**************************************/

    /** MRC Psycholinguistic database. */
    private MRCDatabase  mrcDb;

    /** LIWC dictionary. */
    private LIWCDictionary liwcDic;

    /**
     * Weka ARFF file with all attributes and no instance
     */
    private File attributeFile;


    /** Mapping between long feature names and short ones in the Weka models. */
    private Map<String,String> featureShortcuts;

    /** Arrays of features that aren't included in the models. **/
    private static final String[] domainDependentFeatures = {"FRIENDS", "FAMILY",
            "OCCUP", "SCHOOL", "JOB", "LEISURE", "HOME","SPORTS","TV", "MUSIC", "MONEY",
            "METAPH", "DEATH", "PHYSCAL", "BODY", "EATING", "SLEEP", "GROOM"};


    /** Set of features that aren't included in the models. **/
    private Set<String> domainDependentFeatureSet = new LinkedHashSet<String>(Arrays.asList(domainDependentFeatures));

    /** Arrays of features that aren't included in one instance analysis (corpus analysis only). **/
    private static final String[] absoluteCountFeatures = {"WC"};

    /** Set of features that aren't included in one instance analysis (corpus analysis only). **/
    private Set<String> absoluteCountFeatureSet;

    /** Valid PoS tags in the MRC Psycholinguistic Database. */
    private static final MRCPoS[] MRC_POS = {
            MRCPoS.NOUN, MRCPoS.VERB, MRCPoS.ADJECTIVE, MRCPoS.ADVERB, MRCPoS.PAST_PARTICIPLE,
            MRCPoS.PREPOSITION, MRCPoS.CONJUNCTION, MRCPoS.PRONOUN, MRCPoS.INTERJECTION, MRCPoS.OTHER };


    /**
     * MRC feature names in the model files and in the MRC Psycholinguistic
     * Database.
     */
    private static final Field[] MRC_FEATURES = { MRCDatabase.FIELD_NLET, MRCDatabase.FIELD_NPHON,
            MRCDatabase.FIELD_NSYL, MRCDatabase.FIELD_K_F_FREQ, MRCDatabase.FIELD_K_F_NCATS,
            MRCDatabase.FIELD_K_F_NSAMP, MRCDatabase.FIELD_T_L_FREQ, MRCDatabase.FIELD_BROWN_FREQ,
            MRCDatabase.FIELD_FAM, MRCDatabase.FIELD_CONC, MRCDatabase.FIELD_IMAG,
            MRCDatabase.FIELD_MEANC, MRCDatabase.FIELD_MEANP, MRCDatabase.FIELD_AOA };


    /**
     * Weka personality model files for each trait in the DIMENSION array. All
     * the files specified need to be present in each model's directory.
     */
    private static final String[] DIM_MODEL_FILES = { "extra.model", "ems.model",
            "agree.model", "consc.model", "open.model" };

    /** Line separator. */
    public static final String LS = System.getProperty("line.separator");

    /** File separator. */
    public static final String FS = File.separator;

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    private final BlockingQueue<byte[]> bufferQueue = new LinkedBlockingQueue<>();

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(INPUT_TYPE);
        descriptors.add(MODEL_TYPE);
        descriptors.add(OUTPUT_COUNT_FLAG);
        descriptors.add(OUTPUT_MODEL_FLAG);
        descriptors.add(MRC_PATH);
        descriptors.add(LIWC_PATH);
        descriptors.add(ARFF_PATH);
        descriptors.add(MODELS_BASE_DIR);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

    }


    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        /******************************/
        /** 1. Initialize parameters **/
        /******************************/


        /** MRC Database **/
        try {
            mrcDb = new MRCDatabase(new File(String.valueOf(context.getProperty(MRC_PATH).evaluateAttributeExpressions(flowFile).getValue())));
        } catch (IOException mrcDbE) {
            getLogger().error("Couldn't find MRC DB in path {}. Error: {}", new Object[]{String.valueOf(context.getProperty(MRC_PATH).evaluateAttributeExpressions(flowFile).getValue()), mrcDbE});
            session.transfer(flowFile, REL_FAILURE);
            return;
        }

        /** LIWC Dictionnary **/

        liwcDic = new LIWCDictionary(new File(String.valueOf(context.getProperty(LIWC_PATH).evaluateAttributeExpressions(flowFile).getValue())));


        /** Attribute File **/
        attributeFile = new File(String.valueOf(context.getProperty(ARFF_PATH).evaluateAttributeExpressions(flowFile).getValue()));

        /** Shortcut map **/
        featureShortcuts = getShortFeatureNames();

        // load non general features
        domainDependentFeatureSet = new LinkedHashSet<String>(Arrays.asList(domainDependentFeatures));

        // load absolute features
        absoluteCountFeatureSet = new LinkedHashSet<String>(Arrays.asList(absoluteCountFeatures));

        // 2. Read text from flowfile

        String contentString;
        //byte[] buffer = bufferQueue.poll();
        InputStream in = session.read(flowFile);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            StreamUtils.copy(in, bos);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        contentString = bos.toString();

        /*
        try {
            final byte[] byteBuffer = buffer;
            session.read(flowFile, new InputStreamCallback() {
                @Override
                public void process(InputStream in) throws IOException {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    StreamUtils.copy(in, bos);
                    contentString = bos.toString();
                }
            });

            final long len = Math.min(byteBuffer.length, flowFile.getSize());
            contentString = new String(byteBuffer, 0, (int) len, Charset.forName("UTF-8"));



        } finally {
            bufferQueue.offer(buffer);
        }

*/

        //BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(session.read(flowFile)));
        try {
            //String text = bufferedReader.readLine();


            // load models for all Big Five traits
            Classifier[] models = loadWekaModels(String.valueOf(context.getProperty(MODEL_TYPE).evaluateAttributeExpressions(flowFile).getValue()),
                                                 String.valueOf(context.getProperty(INPUT_TYPE).evaluateAttributeExpressions(flowFile).getValue()).equals(WRITTEN_CONTENT),
                                                true,
                                                 String.valueOf(context.getProperty(MODELS_BASE_DIR).evaluateAttributeExpressions(flowFile).getValue())
                                                 );

            getLogger().debug(contentString);
            // get feature counts from the input text
            Map<String,Double> counts = getFeatureCounts(contentString);
            getLogger().debug("Total features computed: " + counts.size());

            // if flag is set to yes, we output the counts to mairessepersonalityrecognition.count_output
            if (String.valueOf(context.getProperty(OUTPUT_COUNT_FLAG).evaluateAttributeExpressions(flowFile).getValue()).equals(YES)) {
                flowFile = session.putAttribute(flowFile, COUNT_OUTPUT, Utils.printMapToString(counts));
            }



            // compute the personality scores of the new instance for each trait
            getLogger().debug("Running models...");
            double[] scores = runWekaModels(models, counts);

            getLogger().debug("HERE WE GO!");
            // Putting Results in attributes

            flowFile = session.putAttribute(flowFile, EXTRAVERSION, ""+scores[0]);
            flowFile = session.putAttribute(flowFile, EMOTIONAL_STABILITY, ""+scores[1]);
            flowFile = session.putAttribute(flowFile, AGREEABLENESS, ""+scores[2]);
            flowFile = session.putAttribute(flowFile, CONSCIENTIOUSNESS, ""+scores[3]);
            flowFile = session.putAttribute(flowFile, OPENNESS_TO_EXPERIENCE, ""+scores[4]);


            // Printing output of models if flag is set to yes

            if (String.valueOf(context.getProperty(OUTPUT_MODEL_FLAG).evaluateAttributeExpressions(flowFile).getValue()).equals(YES)) {
                String model_out = "Models output \n" +
                               "Extraversion: " + models[0].toString() + "\n"+
                               "Emotional Stability: " + models[1].toString() + "\n"+
                               "Agreeableness: " + models[2].toString() + "\n"+
                               "Conscientiousness: " + models[3].toString() + "\n"+
                               "Openness to experience: " + models[3].toString() + "\n";

                flowFile = session.putAttribute(flowFile, MODEL_OUTPUT, model_out);
            }

            session.transfer(flowFile, REL_SUCCESS);


        } catch (IOException readFileE) {
            getLogger().error("Couldn't read file from flowfile {}. Error: {}", new Object[]{flowFile, readFileE});
            session.transfer(flowFile, REL_FAILURE);
            return;
        } catch (Exception e) {
            getLogger().error("Flowfile {} execution failed. Error: {}", new Object[]{flowFile, e});
            session.transfer(flowFile, REL_FAILURE);
            return;
        }


    }








    /**
     * Computes the features from the input text (70 LIWC features and 14 from
     * the MRC database).
     *
     * @param text
     *            input text.
     * @return mapping associating feature names strings with feature counts
     *         (Double objects).
     * @throws Exception
     */
    private Map<String,Double> getFeatureCounts(String text) throws Exception {


        Map<String,Double> counts = new LinkedHashMap<String,Double>();

        // compute LIWC and MRC features
        Map<String,Double> initCounts = liwcDic.getCounts(text, true);

        for (String longFeature : initCounts.keySet()) {

            if (featureShortcuts.containsKey(longFeature)) {
                counts.put(featureShortcuts.get(longFeature), initCounts.get(longFeature));
            } else {
                counts.put(longFeature, initCounts.get(longFeature));
                // System.err.println("Warning: LIWC feature " + longFeature + " not recognized, check LIWC.CAT file");
            }
        }

        // remove domain dependent LIWC features
        counts.keySet().removeAll(domainDependentFeatureSet);
        getLogger().debug("LIWC features computed: " + counts.size());

        // compute MRC features
        Map<String,Double> mrcFeatures = getMRCCounts(mrcDb, text);
        counts.putAll(mrcFeatures);
        getLogger().debug("MRC features computed: " + mrcFeatures.size());

        return counts;
    }





    /**
     * Computes the average value of MRC features for all words in the text.
     * Ratio is computed over the words with a non-zero value. Does not check
     * for the word PoS, a word is associated with the feature value of the
     * first homonym with a PoS in the MRC_POS array.
     *
     * @param db
     *            mapping associating each word with a line of the MRC Psycholinguistic Database.
     * @param text
     *            input text.
     * @return mapping associating each 14 MRC feature name with the average
     *         value of that feature for all words with non null values in the
     *         input text (Double objects).
     */
    private Map<String,Double> getMRCCounts(MRCDatabase db, String text) throws QueryException {

        // tokenize text
        String[] words = LIWCDictionary.tokenize(text);

        Map<String,Double> counts = new LinkedHashMap<String, Double>(MRC_FEATURES.length);
        Map<String,Integer> nonzeroWords = new LinkedHashMap<String, Integer>(MRC_FEATURES.length);

        // initialize counts
        for (int i = 0; i < MRC_FEATURES.length; i++) {
            counts.put(MRC_FEATURES[i].toString(), 0.0);
            nonzeroWords.put(MRC_FEATURES[i].toString(), 0);
        }

        for (int i = 0; i < words.length; i++) {

            if (db.containsWord(words[i])) {
                Set<PoS> posSet = db.getAvailablePoS(words[i]);

                // only consider the first PoS in the order specified by MRC_POS
                for (int j = 0; j < MRC_POS.length; j++) {

                    if (posSet.contains(MRC_POS[j])) {
                        // update counts for all fields in the database
                        for (int k = 0; k < MRC_FEATURES.length; k++) {
                            try {
                                counts.put(MRC_FEATURES[k].toString(), counts
                                        .get(MRC_FEATURES[k].toString())
                                        + db.getValue(words[i], MRC_POS[j],
                                        MRC_FEATURES[k]));
                                // update non zero words if no exception
                                nonzeroWords.put(MRC_FEATURES[k].toString(),
                                        nonzeroWords.get(MRC_FEATURES[k]
                                                .toString()) + 1);

                            } catch (UndefinedValueException e) {
                                // proceed to next field
                            } catch (EntryNotFoundException e) {
                                getLogger().error("Warning: entry " + words[i]
                                                + "/" + MRC_POS[j].toString() + "/"
                                                + MRC_FEATURES[k].toString()
                                                + " not found");
                            }
                        }
                        // PoS matched, proceed to next word
                        break;
                    }
                }
            }
        }

        // get ratio of feature counts over all non zero words
        for (String feature : counts.keySet()) {
            if (nonzeroWords.get(feature) != 0) {
                counts.put(feature, counts.get(feature)
                        / nonzeroWords.get(feature));
            } else {
                counts.put(feature, Double.NaN);
            }
        }

        return counts;
    }


    /**
     * Loads a Weka model in memory, from a file saved through the Weka GUI.
     * JRE/JDK and Weka versions need to be the same or compatible when saving
     * the models and loading them.
     *
     * @param modelFile
     *            saved Weka model.
     * @return Weka model object.
     * @throws Exception
     */
    private Classifier loadWekaModel(File modelFile) throws Exception {

        getLogger().debug("Loading model " + modelFile.getAbsolutePath()
                + "...");
        InputStream is = new FileInputStream(modelFile);
        ObjectInputStream objectInputStream = new ObjectInputStream(is);
        Classifier classifier = (Classifier) objectInputStream.readObject();
        objectInputStream.close();

        // String className = classifier.getClass().getName();
        // System.err.println("class: " + className);
        return classifier;
    }


    public Classifier[] loadWekaModels(String model, boolean selfModel, boolean stdModels, String modelsBaseDir) {

        String modelDirectory = "";

        switch (model) {
            case LINEAR_REGRESSION:
                modelDirectory = "LinearRegression";
                break;
            case M5_MODEL_TREE:
                modelDirectory = "M5P";
                break;
            case M5_REGRESSION_TREE:
                modelDirectory = "M5P-R";
                break;
            case SMOREG:
                modelDirectory = "SVM";
                break;
        }


        // change file name for standardized models
        String stdPrefix = "";
        if (stdModels) { stdPrefix = "std-"; }
        String modelType = "obs";
        if (selfModel) { modelType = "self"; }

        Classifier[] models = new Classifier[DIM_MODEL_FILES.length];
        try {
            // for each personality trait
            for (int i = 0; i < DIM_MODEL_FILES.length; i++) {
                // get model class based on parameter string
                models[i] = loadWekaModel(new File(modelsBaseDir + FS + modelType + FS + modelDirectory + FS + stdPrefix + DIM_MODEL_FILES[i]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return models;
    }

    /**
     * Runs each Weka model on a new instance created from the input feature
     * counts, and outputs the resulting personality score.
     *
     * @param models
     *            array of Weka models (Classifier objects).
     * @param counts
     *            mapping of feature counts (Double objects), it must probide
     *            a value for all attribute strings of the input models.
     * @return an array containing a personality score for each model.
     */
    public double[] runWekaModels(Classifier[] models, Map<String,Double> counts) {
        double[] scores = new double[models.length];
        try {
            // for each model
            for (int i = 0; i < models.length; i++) {
                // compute score based on loaded model and counts
                scores[i] = runWekaModel(models[i], counts);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scores;
    }


    /**
     * Runs a Weka model on the input feature (attribute) counts and returns the
     * model's score.
     *
     * @param model
     *            Weka model to use for regression. It must have been trained
     *            using the same attribute set as the keys of the userData
     *            hashtable, plus the class attribute. The class must be
     *            numeric.
     * @param userData
     *            test instance, as a hashtable associating attribute strings of
     *            the model with attribute values (Double objects).
     * @return numeric output of the model.
     */
    private double runWekaModel(Classifier model, Map<String,Double> userData)
            throws Exception {

        // create new instance from test data
        Instance userInst = new Instance(userData.size() + 1);
        // create empty dataset
        Instances dataset = new Instances(new BufferedReader(new FileReader(
                attributeFile)), 1);
        if (userData.size() < dataset.numAttributes() - 1) { dataset.deleteAttributeAt(dataset.attribute("WC").index()); }

        userInst.setDataset(dataset);
        dataset.setClassIndex(dataset.numAttributes() - 1);

        for (Attribute attr : Collections.list((Enumeration<Attribute>) dataset.enumerateAttributes())) {

            if (userData.containsKey(attr.name().toUpperCase())) {
                if (userData.get(attr.name().toUpperCase()).toString().equals(
                        "?")) {
                    userInst.setMissing(attr);
                    getLogger().debug("Warning: attribute " + attr.name()
                            + " missing");
                } else {
                    double attrValue = userData.get(attr.name()
                            .toUpperCase());
                    userInst.setValue(attr, attrValue);
                }

            } else {
                getLogger().debug("No value for feature " + attr.name()
                        + ", setting as missing value...");
                userInst.setMissing(attr);
            }
        }
        userInst.setClassMissing();

        // run model for test data
        double result = model.classifyInstance(userInst);
        return result;
    }


    /**
     * Returns a mapping associating features in the LIWC.CAT file to shortcuts used in the
     * Weka models.
     *
     * @return mapping asssociating long names in the LIWC dictionary to the short names in the
     * Weka models.
     */
    private Map<String,String> getShortFeatureNames() {

        Map<String,String> shortcuts = new LinkedHashMap<String,String>();

        shortcuts.put("LINGUISTIC", "LINGUISTIC");
        shortcuts.put("PRONOUN", "PRONOUN");
        shortcuts.put("I", "I");
        shortcuts.put("WE", "WE");
        shortcuts.put("SELF", "SELF");
        shortcuts.put("YOU", "YOU");
        shortcuts.put("OTHER", "OTHER");
        shortcuts.put("NEGATIONS", "NEGATE");
        shortcuts.put("ASSENTS", "ASSENT");
        shortcuts.put("ARTICLES", "ARTICLE");
        shortcuts.put("PREPOSITIONS", "PREPS");
        shortcuts.put("NUMBERS", "NUMBER");
        shortcuts.put("PSYCHOLOGICAL PROCESS", "PSYCHOLOGICAL PROCESS");
        shortcuts.put("AFFECTIVE PROCESS", "AFFECT");
        shortcuts.put("POSITIVE EMOTION", "POSEMO");
        shortcuts.put("POSITIVE FEELING", "POSFEEL");
        shortcuts.put("OPTIMISM", "OPTIM");
        shortcuts.put("NEGATIVE EMOTION", "NEGEMO");
        shortcuts.put("ANXIETY", "ANX");
        shortcuts.put("ANGER", "ANGER");
        shortcuts.put("SADNESS", "SAD");
        shortcuts.put("COGNITIVE PROCESS", "COGMECH");
        shortcuts.put("CAUSATION", "CAUSE");
        shortcuts.put("INSIGHT", "INSIGHT");
        shortcuts.put("DISCREPANCY", "DISCREP");
        shortcuts.put("INHIBITION", "INHIB");
        shortcuts.put("TENTATIVE", "TENTAT");
        shortcuts.put("CERTAINTY", "CERTAIN");
        shortcuts.put("SENSORY PROCESS", "SENSES");
        shortcuts.put("SEEING", "SEE");
        shortcuts.put("HEARING", "HEAR");
        shortcuts.put("FEELING", "FEEL");
        shortcuts.put("SOCIAL PROCESS", "SOCIAL");
        shortcuts.put("COMMUNICATION", "COMM");
        shortcuts.put("REFERENCE PEOPLE", "OTHREF");
        shortcuts.put("FRIENDS", "FRIENDS");
        shortcuts.put("FAMILY", "FAMILY");
        shortcuts.put("HUMANS", "HUMANS");
        shortcuts.put("RELATIVITY", "RELATIVITY");
        shortcuts.put("TIME", "TIME");
        shortcuts.put("PAST", "PAST");
        shortcuts.put("PRESENT", "PRESENT");
        shortcuts.put("FUTURE", "FUTURE");
        shortcuts.put("SPACE", "SPACE");
        shortcuts.put("UP", "UP");
        shortcuts.put("DOWN", "DOWN");
        shortcuts.put("INCLUSIVE", "INCL");
        shortcuts.put("EXCLUSIVE", "EXCL");
        shortcuts.put("MOTION", "MOTION");
        shortcuts.put("PERSONAL PROCESS", "PERSONAL PROCESS");
        shortcuts.put("OCCUPATION", "OCCUP");
        shortcuts.put("SCHOOL", "SCHOOL");
        shortcuts.put("JOB OR WORK", "JOB");
        shortcuts.put("ACHIEVEMENT", "ACHIEVE");
        shortcuts.put("LEISURE ACTIVITY", "LEISURE");
        shortcuts.put("HOME", "HOME");
        shortcuts.put("SPORTS", "SPORTS");
        shortcuts.put("TV OR MOVIE", "TV");
        shortcuts.put("MUSIC", "MUSIC");
        shortcuts.put("MONEY", "MONEY");
        shortcuts.put("METAPHYSICAL", "METAPH");
        shortcuts.put("RELIGION", "RELIG");
        shortcuts.put("DEATH AND DYING", "DEATH");
        shortcuts.put("PHYSICAL STATES", "PHYSCAL");
        shortcuts.put("BODY STATES", "BODY");
        shortcuts.put("SEXUALITY", "SEXUAL");
        shortcuts.put("EATING", "EATING");
        shortcuts.put("SLEEPING", "SLEEP");
        shortcuts.put("GROOMING", "GROOM");
        shortcuts.put("EXPERIMENTAL DIMENSION", "EXPERIMENTAL DIMENSION");
        shortcuts.put("SWEAR WORDS", "SWEAR");
        shortcuts.put("NONFLUENCIES", "NONFL");
        shortcuts.put("FILLERS", "FILLERS");

        return shortcuts;
    }

}
