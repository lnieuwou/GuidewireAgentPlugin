package com.appdynamics.isdk.guidewire;
/**
 * Created by louis.nieuwoudt on 11/06/2018.
 */
public enum MatchOperator {
    EXISTS, EQUALS, STARTS_WITH, ENDS_WITH, CONTAINS ; // , MATCHES ;

    public static MatchOperator fromString( String s)
    {
        for (MatchOperator o : MatchOperator.values())
        {
            if ( o.name().equals( s ))
                return o ;
        }
        return null ;
    }

}

