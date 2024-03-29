/*
 * Copyright (C) 2011 Alexander Forselius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.krikelin.mako;
import com.krikelin.mako.javascript.*;
import java.util.Hashtable;

public class MakoEngine
{
    /// <summary>
    /// Invokes an method on the script
    /// </summary>
    /// <param name="method">method name</param>
    /// <param name="args">arguments passed to the function</param>
    /// <returns></returns>
    public Object Invoke(String method,Object... args)
    {
        return this.RuntimeMachine.invoke(method, args);
    }
    /// <summary>
    /// Returns the old output
    /// </summary>
    public String OldOutput;
    /// <summary>
    /// Raises the create event handler. Useful to add features to the engine before running
    /// </summary>
    /// <param name="sender">The current instance of MakoEngine</param>
    /// <param name="e">EventArg</param>
    public class CreateEventHandler
    {
    	public void invoke(Object sender, EventArgs e)
    	{
    		
    	}
    }
    public CreateEventHandler Create;
    public MakoEngine()
    {
        // Initialize runtime engine. For now we use JavaScriptEngine
        RuntimeMachine = new JavaScriptEngine();

        // Set JSPython to try as default
       // JSPython = true;
        
        // Raise create event handler
        if (this.Create!=null)
        {
            this.Create.invoke(this, new EventArgs());
        }
    }
    public String Output = "";
    /// <summary>
    /// Callback where the output is thrown to, called by the parsed String
    /// </summary>
    /// <param name="values"></param>
    /// <returns></returns>
    public Object __printx( String values)
    {
        Output += values.replace("%BR%","\n").replace("�","\"");
        return true;
    }
    /// <summary>
    /// Synchronize data is called by the javascript preparser to get an ready to use JSON parsed data. If the dat can't be parsed as JSON
    /// it will be returned as an common String
    /// </summary>
    /// <param name="uri">The address to the remote information to retrieve</param>
    /// <returns></returns>
    public Object synchronize_data(String uri)
    {
        /**
         * TODO Fix implementation here
         */
    	return null;
    }

    /// <summary>
    /// Function to convert variable signatures to variable concations for the parser
    /// </summary>
    /// <param name="Line"></param>
    /// <param name="signature"></param>
    /// <returns></returns>
    public String HandleToTokens(String Line, char signature)
    {
        
        new Hashtable<String, Object>();
        // The index of the beginning of an varialbe statement @{
        int IndexOf = 0;
        /**
         * Iterate through all indexes of the @{ statemenet until it ends (IndexOf will return -1  becase IndexOf will gain
         * the new IndexOf with the new statement
         * */
        if(Line.length() > 0)
        while (IndexOf != -1)
        {
            IndexOf = Line.indexOf(signature + "{", IndexOf);
            if (IndexOf == -1)
                break;
            // Gain the index of the next occuranse of the @{ varialbe
            
            int endToken = Line.indexOf('}', IndexOf);

            int startIndex = IndexOf + 2;

            // Get the data inside the token
            String Parseable = Line.substring(startIndex,  endToken -  startIndex);

            // Convert the inline token to concation
            Line = Line.replace("@{" + Parseable + "}",  "\" + ( "  + Parseable + " ) + \"");
            IndexOf = endToken;
           
        }
        return Line;
     

    }
    /// <summary>
    /// This function returns variable from the parser embedded in an output field, asserted with an custom sign {VARNAME}
    /// </summary>
    /// <param name="Line">The code line to execute</param>
    /// <param name="signature">The char signature</param>
    /// <returns>An list of processed variables</returns>
    public Hashtable<String, Object> GetVariables(String Line, char signature)
    {
        Hashtable<String,Object> Variables = new Hashtable<String,Object>();
        // The index of the beginning of an varialbe statement @{
        int IndexOf = 0;
        /**
         * Iterate through all indexes of the @{ statemenet until it ends (IndexOf will return -1  becase IndexOf will gain
         * the new IndexOf with the new statement
         * */
        while (IndexOf != -1)
        {
            // Gain the index of the next occuranse of the @{ varialbe
            IndexOf  = Line.indexOf(signature+"{");
            int endToken = Line.indexOf("}", IndexOf);

            int startIndex = IndexOf+2;

            // Get the data inside the token
            String Parseable = Line.substring(startIndex, startIndex + endToken - 1);

            // Convert it into an variable
            String[] Result = ExecuteScalarVariable(Parseable, ":", "|", true);
            Variables.put(Result[0], Result[1]);
        }
        return Variables;
       
    }
    /// <summary>
    /// This function executes the scalar variable, works together with GetVariable. It will also parse the inherited 
    /// codebase.
    /// </summary>
    /// <param name="Variable"></param>
    /// <param name="reflector">Reflector divides which is the conditinoal statement and the boolean output</param>
    /// <param name="divider">Boolean divider</param>
    /// <param name="vetero">Which variable beside the reflector divider should be present in fallback</param>
    /// <value>Returns an 2 field String array where {InitialVariableName,Output}</value>
    /// <returns></returns>
    public String[] ExecuteScalarVariable(String Variable,String reflector,String divider,boolean vetero)
    {
        // An variable in this instruction {boolVar} : return1 | return 2
        if (Variable.contains(reflector) && Variable.contains(divider))
        {
            // Get the codebase
            String Codebase = Variable.split(reflector)[0];

            // Run the codebase
            Object d = RuntimeMachine.run(Codebase);

            // If it are an boolean decide it, otherwise return the left/right variable as fallback decided by the vetero varialbe
            if (d instanceof Boolean)
            {
                // Get the two case output
                String[] c = Variable.split(reflector)[1].split(divider);

                // Return the decition
                String output =  (Boolean)d ? c[0] : c[1];
                return new String[] { Codebase, output };
            }
            else
            {
                String[] c = Variable.split(reflector)[1].split(divider);

                // Return the case fallback
                String output = vetero ? c[0] : c[1];
                return new String[] { Codebase, output };
            }
        }
       
        /**
            * Otherwise return the value of the variable asserted by the current state of the execution instance
            * */
      
        // Output data
        Object _output = RuntimeMachine.run("return " + Variable + ";");
        if (_output instanceof String)
        {
            return new String[]{Variable,(String)_output};
        }

        return new String[]{Variable,Variable};
       
        
    }
    /// <summary>
    /// The javascript will be like as python
    /// </summary>
    public boolean JSPython;
    /// <summary>
    /// Instance of the Jint engine running at runtime
    /// </summary>
    public IScriptEngine RuntimeMachine;
    /// <summary>
    /// This function executes String in the js mako engine
    /// </summary>
    /// <param name="e"></param>
    public void Execute(String e)
    {
       
        RuntimeMachine.run(e);
    }
    /// <summary>
    /// Event args for RequestOverLayEventArgs
    /// </summary>
    public class OverlayEventArgs
    {
        /// <summary>
        /// The view URI
        /// </summary>
        public String URI ;
        /// <summary>
        /// Folders for the views for each engine. You must provide it in the event attached
        /// by the MediaChrome Host (or apporiate implementation)
        /// </summary>
        public Hashtable<String, String> ViewFolders ;

        /// <summary>
        /// Gets or sets if the operation should be cancelled or not
        /// </summary>
        public boolean Cancel ;
    }
    public class OverlayEventHandler {
    	public void invoke (Object sender, OverlayEventArgs e)
    	{
    		
    	}
    }
    /// <summary>
    /// Occurs on request overlay
    /// </summary>
    public OverlayEventHandler RequestOverlay;

    /// <summary>
    /// Occurs when overlay has been finished request
    /// </summary>
    public OverlayEventHandler OverlayApplied;
    /// <summary>
    /// This function preprosses the mako layer
    /// </summary>
    /// <param name="input">The input String to parse</param>
    /// <param name="argument">The argument sent to the parser</param>
    public String Preprocess(String input, String argument, boolean inflate, String uri, boolean onlyPreprocess)
    {
        //#region OverlayManager Experimental
        /****
         * STOCKHOLM 2011-07-01 14:45
         * 
         * New feature: Apply custom overlays specified by individual service:
         * Overlay are marked with <#namespace#> where "namespace" is an special
         * kind of <namespace>.xml view file inside an <extension_dir>/views/ folder
         * */
        
        // First gain attention by the event

        OverlayEventArgs args = new OverlayEventArgs(); // Create event args
        args.URI = uri;
        if (RequestOverlay != null)
            RequestOverlay.invoke(this, args);

        // if the args has retained the phase, eg. not cancelled
        if (!args.Cancel)
        {
            // Substitute the overlay placeholders with the views
            if(args.ViewFolders != null)
            {
            	
            }
         /*   foreach (KeyValuePair<String, String> overlay in args.ViewFolders)
            {
                using (StreamReader SR = new StreamReader(overlay.Value))
                {
                    String content = SR.ReadToEnd();
                    // Substitute overlay placeholders
                    input = input.replace("<#" + overlay.Key.replace(".xml","") + "#>", content);
                    SR.Close();
                }
            }*/
        }

        // Remove unwanted trailings
      /*  Regex a = new Regex(@"<\#[^\#]*\#>", RegexOptions.IgnoreCase);
        input = a.replace(input, "");*/
        //#endregion

        /**
         * Begin normal operation
         * */


        // Clear the output buffer
        Output = "";
        /**
         * Tell the runtime machine about the argument
         * */
        try
        {
            String[] arguments = argument.split(":");
            RuntimeMachine.setVariable("parameter", argument.replace(arguments[0] + ":", "").replace(arguments[1] + ":", ""));
            RuntimeMachine.setVariable("service", arguments[0]);
        }
        catch(Exception e) { }
        /**
         * This String defines the call-stack of the query
         * This is done before any other preprocessing
         * */
        String CallStack = "";
        input.split("\n");
        /**
         * Boolean indicating which parse mode the parser is in,
         * true = in executable text
         * false = in output line 
         * */
        boolean parseMode = false;
        /***
         * Iterate through all lines and preprocess the page.
         * If page starts with an % it will be treated as an preparser code or all content
         * inside enclosing <? ?>
         * Two String builders, the first is for the current output segment and the second for the current
         * executable segmetn
         * */
        StringBuilder outputCode =  new StringBuilder();
        StringBuilder executableSegment = new StringBuilder();

        // The buffer for the final preprocessing output
        StringBuilder finalOutput = new StringBuilder();
        // Append initial case
        outputCode.append("");

        // Boolean startCase. true = <? false \n%
        boolean startCase = false;

        // Boolean which tells if the cursor is in the preparse or output part of the buffer (inside or outside an executable segment)
        boolean codeMode = false;
        for(int i=0; i < input.length() ;i++)
        {
        	
             // Check if at an overgoing to an code block
            if(codeMode)
            {
                if((startCase && input.charAt(i) == '?' && input.charAt(i+1) == '>') ||( input.charAt(i) == '\n' && !startCase))
                {
          
                    codeMode=false;

                    // Jump forward two times if endcase is ?>
                    if(startCase)
                        i++;

                    // Get the final output
                    String codeOutput = executableSegment.toString();
                    // If in JSPython mode, convert all row breaks to ; and other syntax elements
                    if (JSPython)
                    {
                        codeOutput = codeOutput.replace("\n", ";");
                        
                        /**
                         * Convert statements
                         * */
                        codeOutput = codeOutput.replace(":", "{");
                        codeOutput = codeOutput.replace("end", "}");
                       
                        codeOutput = codeOutput.replace("\nif ", "\nif(");
                        codeOutput = codeOutput.replace("then:", "){");
                        codeOutput = codeOutput.replace("do:", "){");

                        codeOutput = codeOutput.replace("endif", "}");
                        


                    }
                    codeOutput = codeOutput.replace("lt;", "<");
                   

                    codeOutput = codeOutput.replace("lower than", "<");
                    codeOutput = codeOutput.replace("lower", "<");

                    codeOutput = codeOutput.replace("higher", ">");

                    codeOutput = codeOutput.replace("highter than", ">");
                    codeOutput = codeOutput.replace("gt;", ">");
                    // Append the code data to the String buffer
                    finalOutput.append(" "+ codeOutput  + " ");
                    
                    

                    // Clear outputcode buffer
                    executableSegment = new StringBuilder();

                    continue;
                }
                executableSegment.append(input.charAt(i));
            }
            else
            {
                // If at end, summarize the String
                if(i == input.length() - 1)
                {
                    // Append the last String
                    outputCode.append(input.charAt(i));
                    // Format output code (Replace " to � and swap back on preprocessing)
                    String OutputCode = outputCode.toString().replace("\"", "�").replace("\n", "%BR%\");\n__printx(\"");
                    OutputCode = this.HandleToTokens(OutputCode.toString(),'@');
                    finalOutput.append("__printx(\"" + OutputCode + "\");");
                   
                }
                try
                {
                    if (((input.charAt(i) == '\n' && input.charAt(i + 1) == '%')) || (input.charAt(i) == '<' && input.charAt(i + 1) == '?'))
                    {
                        startCase = (input.charAt(i) == '<' && input.charAt(i + 1) == '?');
                        codeMode = true;

                        // Convert tokens to interpretable handles
                        String OutputCode = outputCode.toString().replace("\"", "�").replace("\n", "%BR%\");\n__printx(\"");
                        OutputCode = this.HandleToTokens(OutputCode.toString(), '@');
                        finalOutput.append("__printx(\"" + OutputCode + "\");");

                        // Clear the output code buffer
                        outputCode = new StringBuilder();

                        // Skip two tokens forward to not include those tokens to the code buffer
                        i += 1;
                        continue;
                    }

                }
                catch(Exception e)
                {
                    continue;
                }
                outputCode.append(input.charAt(i));
                
            }
            

        }            
        // if exiting in text mode, append an end of the scalar String
        if (!parseMode)
        {
            CallStack += "\");";
        }
        // Run the code
        RuntimeMachine.setFunction("__printx",new FunctionDelegate(this));
        RuntimeMachine.setFunction("synchronize_data", new FunctionDelegate(this));
        CallStack = finalOutput.toString();
       
        CallStack = CallStack.replace("\r", "");
        if (!onlyPreprocess)
        {
            /***
             * Try run the page. If there was error return ERROR: <error> message so the
             * handler can choose to present it to the user
             * */
            try
            {
                RuntimeMachine.run(CallStack);

                /**
                 * Check if the result of the preprocessing is the same as before. If nothing
                 * has changed return NONCHANGE. This is only for rendering whole pages, not inflate.
                 * */
                if (!inflate)
                {
                    if (Output == OldOutput)
                        return "NONCHANGE";

                    OldOutput = Output;
                }
            }
            catch (Exception e)
            {
                // clear output
                this.Output = "";
                // Load error page
              /*  using (System.IO.StreamReader SR = new System.IO.StreamReader("views\\error.xml"))
                {
                    String errorView = new MakoEngine().Preprocess(SR.ReadToEnd(), "", false, "", true);
                    RuntimeMachine = new JavaScriptEngine();
                    RuntimeMachine.SetFunction("__printx", new Func<String, Object>(__printx));
                    RuntimeMachine.SetVariable("error", e.ToString() + "\n " );

                    RuntimeMachine.Run((errorView));
                }*/
            }
            return this.Output;
        }
           
        else
        {
            return CallStack;
        }

    }
    public class FunctionDelegate extends Delegate
    {
    	private MakoEngine mContext;
    	public FunctionDelegate (MakoEngine context)
    	{
    		mContext=context;
    	}
    	@Override
    	public void invoke(Object... params)
    	{
    		if(params != null)
    			if(params[0] instanceof String)
    				mContext.__printx((String)params[0]);
    	}
    	
    }
}
