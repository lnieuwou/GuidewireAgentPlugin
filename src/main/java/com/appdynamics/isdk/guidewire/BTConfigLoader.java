package com.appdynamics.isdk.guidewire;

import com.appdynamics.instrumentation.sdk.logging.ISDKLogger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by louis.nieuwoudt on 12/06/2018.
 * Helper class to read config CSV file and return a list of BTServletMatch rules
 * If there are errors in the config, the loader will log them and continue is possible
 */
public class BTConfigLoader
{
    private String configFileName ;
    private ISDKLogger logger ;
    public  List<BTServletMatch> rules ;
    public  boolean enableDebug = false ; // true is debug flag was set in the config

    // State variable used while parsing the CSV file
    final int BT_NAME_FIELD     = 0 ;
    final int PARAMERTER_FIELD  = 1 ;
    final int OPERAND_FIELD     = 2 ;
    final int VALUE_FIELD       = 3 ;
    final int MIN_NUMBER_OF_FIELDS = 3 ;

    private String lastBtName ; // Name of the previous BT row processed
    private int conditionRow ;  // Current Count of how CSV file rows referring to the same BT
    private BTServletMatchConditionBase lastCondition ; // Last condition processed by the parser
    private String matchConditionError ;

    public BTConfigLoader(String configFileName, ISDKLogger logger )
    {
        this.configFileName = configFileName ;
        this.logger = logger ;
    }

    // Pass in a reference to "enableDebug" to enable debug logging at runtime
    // The config loader will look for # DEBUG=(true|false) in the first line of the log file
    public List<BTServletMatch> ParseConfig()
    {
        Integer lineNumber = 0 ;
        BufferedReader br = null;
        String line ;
        String cvsSplitBy = ",";
        String comment = "#" ;
        String rawLine = "" ;
        StringBuilder loggerText = new StringBuilder() ;

        try
        {
            // Read the config CSV file
            br = new BufferedReader(new FileReader( configFileName ));
            beginProcessing() ;
            while ((line = br.readLine()) != null)
            {
                rawLine = line ;
                lineNumber++ ;

                // Check if debug is enabled !
                if( lineNumber == 1 )
                {
                    line = line.trim() ;
                    if ( line.matches( "(?i)#\\s*debug\\s*=\\s*true.*")) {
                        enableDebug = true;
                        logger.info("DEBUG is enabled");
                    }
                }
                // Remove all comments and Trim the line
                line = line.replaceAll("#.*", "").trim() ;


                // Skip over empty lines
                if( line.length() == 0 )
                    continue;

                // split the line into fields and parse the content to create BTMatch objects
                try
                {
                    processFields(line.split(cvsSplitBy));
                }
                catch( Exception e)
                {
                    loggerText.append("Error processing Plugin config - Line No " + lineNumber.toString() + " :" + rawLine) ;
                }
            }
            endProcessing() ;

        }
        catch (FileNotFoundException e)
        {
            loggerText.append( "Error reading Plugin config" + e.toString()) ;
        }
        catch (IOException e)
        {
            loggerText.append("Error processing Plugin config - Line No " + lineNumber.toString() + " :" + rawLine + "\n" + e.toString() ) ;
        }
        finally
        {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }

            logger.info( loggerText.toString() );
        }
        return rules ;
    }

    // Initialize the state variables used while parsing the config
    private void beginProcessing()
    {
        rules = new ArrayList<BTServletMatch>();
        lastBtName = "" ;
        lastCondition = null ;
        conditionRow = 0 ;
    }

    private void endProcessing()
    {
        // Add the last row to the list
        if( lastBtName.length() > 0 && lastCondition != null )
            rules.add(new BTServletMatch(lastBtName, lastCondition ));
    }

    // Create a BTServletParameterMatchCondition object based on the values in the fields of one
    // CSV file row.
    private BTServletParameterMatchCondition getMatchCondition( String [] fields )
    {
        matchConditionError = "" ;

        // Make sure we have the minimum number of required fields.
        if ( fields.length < MIN_NUMBER_OF_FIELDS )
        {
            matchConditionError = "Too few fields provided. The first 3 are compulsory" ;
            return null ;
        }
        // Get the parameter name and value
        String paramName = fields[ PARAMERTER_FIELD ].trim() ;

        String value = "" ;
        if( fields.length > VALUE_FIELD )
            value = fields[ VALUE_FIELD ].trim() ;

        // Parse the operand
        String operandString = fields[ OPERAND_FIELD ].trim().toUpperCase() ;
        boolean invertLogic = false ;
        if ( operandString.startsWith("NOT "))
        {
            invertLogic = true ;
            operandString = operandString.replaceAll("NOT\\s+", "") ; // Strip off the NOT
        }
        operandString = operandString.replaceAll("\\s+", "_") ;
        MatchOperator op = MatchOperator.fromString( operandString ) ;

        // Check that we got a valid operand back
        if( op == null )
        {
            matchConditionError = "Invalid operand : '" + operandString +
                    "', Supported values are EXISTS, EQUALS, STARTS_WITH, ENDS_WITH, CONTAINS" ;
            return null ;
        }
        else
        {
            // Check if STARTS_WITH, ENDS_WITH and CONTAINS ave a valid comprison value
            if ((op == MatchOperator.STARTS_WITH || op == MatchOperator.ENDS_WITH || op == MatchOperator.CONTAINS) &&
                    value.length() == 0)
            {
                matchConditionError = "No comparison value provided for operand " + operandString ;
                return null ;

            }
            else
                return new BTServletParameterMatchCondition(op, paramName, value, invertLogic, true);
        }
    }

    // Process the fields of one CSV file row.
    private void processFields( String [] fields ) throws Exception
    {
        // Check if we are still looking at the same BT

        String btName = fields[ BT_NAME_FIELD ] ;
        BTServletParameterMatchCondition mc = getMatchCondition( fields ) ;

        if( ! lastBtName.equals( btName ) ) // started a new expression,
        {
            conditionRow = 0 ;

            if( lastBtName.length() > 0 && lastCondition != null )
            {
                // wrap things up for the previous expression
                rules.add(new BTServletMatch(lastBtName, lastCondition ));
            }

            lastBtName = btName ;
            lastCondition = mc ;
        }
        else
        {
            if( lastCondition != null && mc != null ) // Only proceed if there has not been a config error
            {
                BTServletAndMatchCondition andCond;
                // If this is index 1, then convert the condition to an AND
                if (conditionRow == 1) {
                    andCond = new BTServletAndMatchCondition();
                    andCond.addCondition(lastCondition);
                    lastCondition = andCond;
                }
                andCond = (BTServletAndMatchCondition) lastCondition;
                andCond.addCondition(mc);
            }
            else
            {
                lastCondition = null ; // Propagate the fact that this BT def has an error
            }
        }

        conditionRow++ ;

        // propagate the fields parse errors
        if( mc == null )
            throw new Exception( matchConditionError ) ;
    }

}

