package me.onethecrazy.util.objects.save;

public class FBXPlayerModelsSave {
    public ClientSkin selectedSkin;
    public boolean isEnabled;
    public boolean renderSelfModelInFirstPerson;

    public FBXPlayerModelsSave(){
        this.selectedSkin = new ClientSkin();
        this.isEnabled = true;
        this.renderSelfModelInFirstPerson = false;
    }
}
