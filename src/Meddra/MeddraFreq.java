package Meddra;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MeddraFreq {
    // this file etablish the connection to Meddra Table Side effect
    // here we also use querry to get our result using side effect
    /* connexion informations */
    private static String DB_SERVER = "jdbc:mysql://neptune.telecomnancy.univ-lorraine.fr/";
    private static String DB = "gmd";
    private static String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static String USER_NAME = "gmd-read";
    private static String USER_PSWD = "esial";
    private ArrayList<ArrayList<String>> result;
    public static int numberRequest=0;
    public static int lostRequestByChemical=0;
    public static int lostRequestByATC=0;
    public static float mappingPrecision=0;

    public MeddraFreq(){
        result=new ArrayList<ArrayList<String>>();
    }

    public ArrayList<ArrayList<String>> getMeddra(String sideEffect){
        try {
            Class.forName(DRIVER).newInstance();
            Connection con = DriverManager.getConnection(DB_SERVER + DB, USER_NAME, USER_PSWD);

            //Aspartate Somnolence ...
            String superQuery1 = "SELECT DISTINCT STITCH_compound_id1, STITCH_compound_id2, side_effect_name, frequency_description FROM `meddra_freq` WHERE " + sideEffect +" ORDER BY `freq_lower_bound` DESC" ;

            Statement st = con.createStatement();

            ResultSet res = st.executeQuery(superQuery1);


            while (res.next()) {
                String stitch1 = res.getString("STITCH_compound_id1");
                String stitch2 = res.getString("STITCH_compound_id2");
                String sd = res.getString("side_effect_name");
                String freq = res.getString("frequency_description");
                ArrayList<String> temp = new ArrayList<String>();
                String temps1= stitch1.substring(0,3);
                String temps2= stitch1.substring(4,stitch1.length());
                temp.add(temps1+"m"+temps2);
                String temps12= stitch2.substring(0,3);
                String temps22= stitch2.substring(4,stitch2.length());
                temp.add(temps1+"m"+temps2);
                temp.add(sd);
                temp.add(freq);
                temp.add("Meddra_Freq");
                this.result.add(temp);
            }

            res.close();
            st.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.result;
    }

    public ArrayList<ArrayList<String>> getMeddraOR(List<String> sideEffect){
        try {
            Class.forName(DRIVER).newInstance();
            Connection con = DriverManager.getConnection(DB_SERVER + DB, USER_NAME, USER_PSWD);

            String myQuery = "SELECT * FROM `meddra_freq`";
            //Aspartate Somnolence ...
            String gestOr="";
            String superQuery1 = "SELECT DISTINCT STITCH_compound_id1, STITCH_compound_id2, side_effect_name, frequency_description FROM `meddra_freq` WHERE `side_effect_name` LIKE '%" + sideEffect +"%' ORDER BY `freq_lower_bound` DESC" ;
            String bestQuery="SELECT DISTINCT STITCH_compound_id1, STITCH_compound_id2, side_effect_name, frequency_description FROM `meddra_freq` WHERE"+ gestOr +"ORDER BY `freq_lower_bound` DESC";
            Statement st = con.createStatement();

            ResultSet res = st.executeQuery(superQuery1);


            while (res.next()) {
                String stitch1 = res.getString("STITCH_compound_id1");
                String stitch2 = res.getString("STITCH_compound_id2");
                String sd = res.getString("side_effect_name");
                String freq = res.getString("frequency_description");
                ArrayList<String> temp = new ArrayList<String>();
                String temps1= stitch1.substring(0,3);
                String temps2= stitch1.substring(4,stitch1.length());
                temp.add(temps1+"m"+temps2);
                String temps12= stitch2.substring(0,3);
                String temps22= stitch2.substring(4,stitch2.length());
                temp.add(temps1+"m"+temps2);
                temp.add(sd);
                temp.add(freq);
                temp.add("Meddra_Freq");
                this.result.add(temp);
            }

            res.close();
            st.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.result;
    }

}
