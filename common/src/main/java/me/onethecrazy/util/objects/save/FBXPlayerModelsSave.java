package me.onethecrazy.util.objects.save;

public class FBXPlayerModelsSave {
    public ClientSkin selectedSkin;
    public boolean isEnabled;

    public FBXPlayerModelsSave(){
        this.selectedSkin = new ClientSkin();
        this.isEnabled = true;
    }
}
