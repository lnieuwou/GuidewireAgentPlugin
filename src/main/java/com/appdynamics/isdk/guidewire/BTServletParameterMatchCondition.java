package com.appdynamics.isdk.guidewire;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Map;

/**
 * Created by louis.nieuwoudt on 11/06/2018.
 * Compares a parameter with a match string. optionally negating the result.
 */
public class BTServletParameterMatchCondition extends BTServletMatchConditionBase
{
    protected MatchOperator op ;
    protected String paramName ;
    protected String rVal ;
    protected boolean negate ;
    protected boolean matchCase ;

    public BTServletParameterMatchCondition( MatchOperator op, String paramName, String rVal, boolean negate, boolean matchCase )
    {
        this.op = op ;
        this.paramName = paramName ;
        this.negate = negate;
        this.matchCase = matchCase ;

        if ( ! matchCase )
            this.rVal = rVal.toUpperCase() ;
        else
            this.rVal = rVal ;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder() ;
        sb.append( "{" + paramName + "} " + (negate?"NOT ":"") + op.toString() + " \"" + rVal + "\"" ) ;
        return sb.toString() ;
    }

    @Override
    public boolean Match(Map<String, String[]> params) {

        String[] vals = null;
        boolean paramExists = false;
        if (params.containsKey(paramName)) {
            vals = params.get(paramName);
            paramExists = true;
        }

        if (op == MatchOperator.EXISTS) {
            if (negate)
                paramExists = !paramExists;
            return paramExists;
        } else if (!paramExists)
            return false;

        // Check all the param values for a match
        for (String val : vals)
        {
            if (! matchCase ) // Ignore the case of the match strings
                val = val.toUpperCase();

            boolean retval = false;

            switch (op) {

                case EQUALS:
                    retval = val.equals(rVal);
                    break;

                case STARTS_WITH:
                    retval = val.startsWith(rVal);
                    break;

                case ENDS_WITH:
                    retval = val.endsWith(rVal);
                    break;

                case CONTAINS:
                    retval = val.contains(rVal);
                    break;
            }

            if (negate)
                retval = !retval;

            if ( retval )
                return retval; // found a match

        }
        return false ; // no match found
    }
}
