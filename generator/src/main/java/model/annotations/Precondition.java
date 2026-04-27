package model.annotations;

import java.util.regex.Pattern;

public class Precondition {

    private String name;
    private String oclExpression;
    private String message;
    private String javaExpression;

    public Precondition(String name, String oclExpression, String message) {
        this.name = name;
        this.oclExpression = oclExpression;
        this.message = message;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOclExpression() { return oclExpression; }
    public void setOclExpression(String oclExpression) { this.oclExpression = oclExpression; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getJavaExpression() { return javaExpression; }
    public void setJavaExpression(String javaExpression) { this.javaExpression = javaExpression; }

    public String getJavaExpression(String selfVar) {
        if (javaExpression == null || selfVar == null || selfVar.equals("this")) {
            return javaExpression;
        }
        return Pattern.compile("this\\.(\\w+)").matcher(javaExpression).replaceAll(mr -> {
            String f = mr.group(1);
            return selfVar + ".get" + Character.toUpperCase(f.charAt(0)) + f.substring(1) + "()";
        });
    }

    @Override
    public String toString() {
        return "Precondition{name='" + name + "', ocl='" + oclExpression + "'}";
    }


}
