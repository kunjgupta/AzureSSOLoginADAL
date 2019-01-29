package sso.azure.kunj.com.azuressologin.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserDetails {

    @SerializedName("@odata.context")
    @Expose
    public String odataContext;

    @SerializedName("id")
    @Expose
    public String id;

    @SerializedName("businessPhones")
    @Expose
    public List<Object> businessPhones = null;

    @SerializedName("displayName")
    @Expose
    public String displayName;

    @SerializedName("givenName")
    @Expose
    public String givenName;

    @SerializedName("jobTitle")
    @Expose
    public Object jobTitle;

    @SerializedName("mail")
    @Expose
    public String mail;

    @SerializedName("mobilePhone")
    @Expose
    public Object mobilePhone;

    @SerializedName("officeLocation")
    @Expose
    public Object officeLocation;

    @SerializedName("preferredLanguage")
    @Expose
    public Object preferredLanguage;

    @SerializedName("surname")
    @Expose
    public String surname;

    @SerializedName("userPrincipalName")
    @Expose
    public String userPrincipalName;

    @Override
    public String toString() {
        return
                "id: " + id + "\n" +
                "displayName: " + displayName + "\n" +
                "givenName: " + givenName + "\n" +
                "mail: " + mail + "\n" +
                "surname: " + surname + "\n" +
                "userPrincipalName: " + userPrincipalName;
    }
}
