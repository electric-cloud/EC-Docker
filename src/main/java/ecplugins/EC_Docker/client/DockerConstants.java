package ecplugins.EC_Docker.client;


import java.util.List;
import java.util.Arrays;


public final class DockerConstants {
    private DockerConstants() {}

    public static final String MAVEN = "Maven";
    public static final String NUGET = "NuGet";
    public static final String NPM = "NPM";
    public static final String GENERIC = "Generic";
    public static final String PHP = "PHP";


    public static final String CONFIG = "config";
    public static final String DESTINATION = "destination";
    public static final String PATH_TO_ARTIFACT = "pathToArtifact";
    public static final String REPOSITORY = "repository";
    public static final String ARTIFACT_NAME = "artifact";
    public static final String ARTIFACT_PATH = "artifactPath";
    public static final String VERSION = "version";
    public static final String OVERWRITE = "overwrite";
    public static final String REPO_TYPE = "repoType";
    public static final String ORG_PATH = "orgPath";
    public static final String MODULE = "module";
    public static final String FILE_ITEG_REV = "fileItegRev";
    public static final String REPO_LAYOUT = "repositoryLayout";
    public static final String FOLDER_ITEG_REV = "folderItegRev";
    public static final String CLASSIFIER = "classifier";
    public static final String EXTENSION = "extension";
    public static final String LATEST_VERSION = "latestVersion";
    public static final String RESULT_PROPERTY = "resultPropertySheet";
    public static final String USE_REPOSITORY_LAYOUT = "useRepositoryLayout";
    public static final String ORG = "org";
    public static final String TYPE = "type";
    public static final String EXTRACT = "extract";
    public static final String TOOL_BASED = "tool_based";
    public static final String REPOSITORY_PATH = "repositoryPath";
    public static final String PROPERTIES = "artifactProperties";

    public static final int TEXTAREA_WIDTH = 75;
    public static final int TEXTAREA_HEIGHT = 3;


    public static final List<String> REPO_TYPE_VALUES = Arrays.asList(MAVEN, NPM, NUGET, PHP, GENERIC);

    public static final List<String> DYNAMICALLY_REQUIRED_ROWS = Arrays.asList(
        EXTENSION,
        REPO_LAYOUT,
        ORG_PATH,
        CLASSIFIER,
        FILE_ITEG_REV,
        FOLDER_ITEG_REV,
        TYPE,
        REPOSITORY_PATH,
        ORG
    );
    public static final List<String> DYNAMICALLY_VISIBLE_ROWS = Arrays.asList(
        EXTENSION,
        REPO_LAYOUT,
        ORG_PATH,
        REPOSITORY_PATH,
        ORG,
        CLASSIFIER,
        FILE_ITEG_REV,
        FOLDER_ITEG_REV,
        TYPE
    );

    public static final boolean ONLY_LAYOUT = true;

    public static final List<String> LAYOUT_ROWS = Arrays.asList(
        ORG_PATH,
        ORG,
        CLASSIFIER,
        FOLDER_ITEG_REV,
        FILE_ITEG_REV,
        TYPE,
        EXTENSION
    );


}
