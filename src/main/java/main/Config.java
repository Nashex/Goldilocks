package main;

import org.apache.commons.configuration.PropertiesConfiguration;
import utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Config {

    private static String path;

    static {

        try {
            path = Utils.getJarContainingFolder(Goldilocks.class) + "//config.properties";
            File file = new File(path);

            //create config if not existing
            if (!file.exists()) {

                file.createNewFile();

                FileInputStream fs = new FileInputStream(path);

                PropertiesConfiguration conf = new PropertiesConfiguration(file);

                conf.load(fs);


                conf.addProperty("TOKEN", "");
                conf.addProperty("ACTIVITY", "PLAYING with Gold");
                conf.addProperty("INSTANCE_OWNER", "189853114285817857");
                conf.addProperty("API_SECRET", "");
                conf.addProperty("PROXY_URL", "");

                conf.save();

            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }


    }

    public static Object get(String option) {
        try{
            FileInputStream fs = new FileInputStream(Utils.getJarContainingFolder(Goldilocks.class) + "//config.properties");

            Properties properties = new Properties();
            properties.load(fs);

            String property = properties.getProperty(option);

            return property;
        } catch ( Exception e ){

        }
        return null;
    }

    public static void setActivity(String activity) {
        try{

            FileInputStream fs = new FileInputStream(path);

            Properties conf = new Properties();

            conf.load(fs);

            conf.setProperty("ACTIVITY", activity);

            conf.store(new FileOutputStream(path),null);
        } catch ( Exception e ){
        }
    }




}
