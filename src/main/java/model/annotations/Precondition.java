package model.annotations;

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

    @Override
    public String toString() {
        return "Precondition{name='" + name + "', ocl='" + oclExpression + "'}";
    }


}
