package com.example.misty;

public class Square {
    private boolean visible;
    private char value;
    //    place holder for image ect.
    public Square(char value)
    {
        this.visible = false;
        this.value = value;
    }
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }
    public boolean getVisible()
    {
        return this.visible;
    }
    public char getValue()
    {
        return this.value;
    }
    public void setValue(char value)
    {
        this.value = value;
    }
    public boolean isBomb()
    {
        return this.value == 'B';
    }
    public boolean isGold()
    {
        return this.value == 'G';
    }

}
