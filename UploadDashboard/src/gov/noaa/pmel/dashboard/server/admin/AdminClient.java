/**
 * 
 */
package gov.noaa.pmel.dashboard.server.admin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import gov.noaa.pmel.dashboard.server.Users;
import gov.noaa.pmel.dashboard.server.Users.UserRole;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.util.PasswordUtils;
import gov.noaa.pmel.tws.client.impl.TwsClientImpl.NoopException;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.cli.CLClient;
import gov.noaa.pmel.tws.util.cli.CLCommand;
import gov.noaa.pmel.tws.util.cli.CLOption;
import gov.noaa.pmel.tws.util.cli.CLOptionValue;
import gov.noaa.pmel.tws.util.cli.CLOptions;
import gov.noaa.pmel.tws.util.cli.CommandProcessor;

/**
 * @author kamb
 *
 */
public class AdminClient extends CLClient {

    private static Logger logger;
    
    private static CLOption opt_username = CLOption.builder().name("user").flag("u").longFlag("username")
                                            .requiredOption(true)
                                            .description("username for user-oriented options").build();
    private static CLOption opt_password = CLOption.builder().name("password").flag("pw").longFlag("password")
                                            .description("new user's password - must conform to password complexity rules").build();
    private static CLOption opt_firstName = CLOption.builder().name("firstName").flag("fn").longFlag("firstname")
                                            .requiredOption(true)
                                            .description("User first name.").build();
    private static CLOption opt_middleName = CLOption.builder().name("middleName").flag("mn").longFlag("middlename")
                                            .description("User middle name or initial.").build();
    private static CLOption opt_lastName = CLOption.builder().name("lastName").flag("ln").longFlag("lastname")
                                            .requiredOption(true)
                                            .description("User last name.").build();
    private static CLOption opt_userOrg = CLOption.builder().name("organization").flag("o").longFlag("org")
//                                            .requiredOption(true)
                                            .description("User organization name.").build();
    private static CLOption opt_email = CLOption.builder().name("email").flag("e").longFlag("email")
                                            .requiredOption(true)
                                            .description("User email.").build();
    private static CLOption opt_phone = CLOption.builder().name("phone").flag("t").longFlag("phone")
                                            .description("User phone. Use \"x####\" to specify an extension.").build();
    
    
    @SuppressWarnings("unused") // found by reflection
    private static CLCommand cmd_addUser = CLCommand.builder().name("add_user")
                                                .command("add")
                                                .description("Add a new user to the dashboard.")
                                                .option(opt_username)
                                                .option(opt_password)
                                                .option(opt_firstName)
                                                .option(opt_lastName)
                                                .option(opt_userOrg)
                                                .option(opt_email)
                                                .option(opt_phone)
                                                .build();
    
    private CLOptions _clOptions;

    /**
     * 
     */
    public AdminClient() {
        // TODO Auto-generated constructor stub
    }

    public void doAdd() {
        System.out.println("Add user");
        try {
            String userid = _clOptions.get(opt_username);
            User existgUser = Users.getUser(userid);
            if ( existgUser != null ) {
                throw new IllegalStateException("User " + userid + " exists!");
            }
            String pw = _clOptions.get(opt_password);
            if ( pw != null ) {
                PasswordUtils.validatePasswordStrength(pw);
            }
            User newUser = User.builder()
                            .username(userid)
                            .firstName(_clOptions.get(opt_firstName))
//                            .middle(_clOptions.get(opt_middleName))
                            .lastName(_clOptions.get(opt_lastName))
                            .email(_clOptions.get(opt_email))
                            .build();
            String newPasswd = PasswordUtils.generateSecurePassword();
            Users.addUser(newUser, newPasswd, UserRole.Groupie);
            System.out.println("User added with temporary password: "+ newPasswd);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }
    
    @Override
    public void doCommand(CLCommand command, Map<CLOption, CLOptionValue> optionValues, List<String> arguments) {
        try {
//            checkOptions(command);
            
            _clOptions = new CLOptions(command, optionValues, arguments);
            
            Method processingMethod = getProcessingMethod(command);
            processingMethod.invoke(this);
            
        } catch (InvocationTargetException itex) {
            if ( itex.getCause() == null ||
                 ! ( itex.getCause() instanceof NoopException )) {
                itex.printStackTrace();
            }
        } catch (NoopException nex) {
            System.out.println(nex.getMessage() + " - Exiting.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param command
     * @return
     * @throws SecurityException 
     * @throws NoSuchMethodException 
     */
    private Method getProcessingMethod(CLCommand command) {
        String methodName = getMethodName(command);
        Method processingMethod = null;
        Class<?> thisClass = this.getClass();
        try {
            processingMethod = thisClass.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException nsx) {
//            Method[] thisMethods = thisClass.getDeclaredMethods();
            Class<?> superClass = thisClass.getSuperclass();
            if ( CommandProcessor.class.isAssignableFrom(superClass)) {
                try {
                    processingMethod = superClass.getDeclaredMethod(methodName);
                } catch (NoSuchMethodException ns2) {
//                    Method[] superMethods = superClass.getDeclaredMethods();
                    throw new IllegalStateException("No processing method \"" + methodName + 
                                                    "\" found for command " + command.command() +
                                                    " in either " + thisClass.getName() + " or " + 
                                                    superClass.getName()); 
                }
            } else {
                throw new IllegalStateException("No processing method \"" + methodName + 
                                                "\" found for command " + command +
                                                " in " + thisClass.getName());
            }
        }
        return processingMethod;
    }

    /**
     * @param command
     * @return
     */
    private static String getMethodName(CLCommand command) {
        String methodName = command.methodName();
        if ( methodName == null ) {
            String commandName = command.command();
            char[] chars = ("do"+commandName).toCharArray();
            chars[2] = String.valueOf(chars[2]).toUpperCase().charAt(0);
            methodName = String.valueOf(chars);
        }
        return methodName;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            System.out.println("Running AdminClient");
            List<String> filteredArgs = preprocessArgs(args); // sets system property and removes -D args
            ApplicationConfiguration.Initialize("oap");
            logger = LogManager.getLogger(AdminClient.class);
            logger.debug("Running AdminClient");
            runCommand(filteredArgs.toArray(new String[filteredArgs.size()]));

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

}
