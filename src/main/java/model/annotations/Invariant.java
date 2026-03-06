package model.annotations;

/***
 * invariante ocl:
 * formato:
 *      context <ClassName> inv <InvariantName>: <OCLExpression>
 *          message: <ErrorMessage>
 */
public class Invariant {
    private String name;
    private String oclExpression;
    private String message;

    public Invariant (String name, String oclExpression, String message) {
        this.name = name;
        this.oclExpression = oclExpression;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOclExpression() {
        return oclExpression;
    }

    public void setOclExpression(String oclExpression) {
        this.oclExpression = oclExpression;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Invariant {" +
                "name:'" + name + '\'' +
                ", oclExpression:'" + oclExpression + '\'' +
                ", message:'" + message + '\'' +
                '}';
    }

}
