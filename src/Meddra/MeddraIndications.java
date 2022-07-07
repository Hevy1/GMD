package Meddra;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MeddraIndications {
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

    public MeddraIndications(){
        result=new ArrayList<ArrayList<String>>();
    }
    // this file etablish the connection to Meddra Table Indication
    // here we also use querry to get our result using side effect
    public ArrayList<ArrayList<String>> getIndication(String sideEffect) throws Exception {
        try {

            Class.forName(DRIVER).newInstance();
            Connection con = DriverManager.getConnection(DB_SERVER + DB, USER_NAME, USER_PSWD);

            String myQuery = "SELECT DISTINCT STITCH_compound_id, concept_name, meddra_concept_name FROM `meddra_all_indications` WHERE "+sideEffect;
            //Aspartate Somnolence ...
            //String superQuery1 = "SELECT DISTINCT STITCH_compound_id1, STITCH_compound_id2, side_effect_name, frequency_description FROM `meddra_freq` WHERE `side_effect_name` LIKE '%" + sideEffect +"%' ORDER BY `freq_lower_bound` DESC" ;

            Statement st = con.createStatement();

            ResultSet res = st.executeQuery(myQuery);


            while (res.next()) {
                String id = res.getString("STITCH_compound_id");
                //String cui = res.getString("cui");
                String concept_name = res.getString("concept_name");
                //String concept_type = res.getString("meddra_concept_type"); useless
                //String cui_meddra = res.getString("cui_of_meddra_term");
                String meddra_concept_name = res.getString("meddra_concept_name");
                //System.out.println(id + "   " + concept_name  + "   " +meddra_concept_name) ;
                //System.out.println(id + "   " + cui + "   " + concept_name  + "   " + cui_meddra +"  "+meddra_concept_name) ;
                ArrayList<String> temp = new ArrayList<String>();
                String temps1= id.substring(0,3);
                String temps2= id.substring(4,id.length());
                temp.add(temps1+"m"+temps2);
                temp.add(concept_name);
                temp.add(meddra_concept_name);
                temp.add("Meddra_Indication");
                this.result.add(temp);
            }

            res.close();
            st.close();
            con.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("La requête a échoué");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new Exception("Vous n'êtes pas connectés au VPN de l'univ lorraine");
        }
        return this.result;
    }

    public ArrayList<ArrayList<String>> Clear(ArrayList<ArrayList<String>> tab){
        ArrayList<ArrayList<String>> newTab =new ArrayList<ArrayList<String>>();
        for(int i=0;i<tab.size();i++){
            if(!tab.get(i).get(0).equals("")){
                newTab.add(tab.get(i));
            }
        }

        return newTab;
    }

}