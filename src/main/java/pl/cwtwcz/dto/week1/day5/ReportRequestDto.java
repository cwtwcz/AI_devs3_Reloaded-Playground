package pl.cwtwcz.dto.week1.day5;

public class ReportRequestDto {
    private String task;
    private String apikey;
    private String answer;

    public ReportRequestDto() {}

    public ReportRequestDto(String task, String apikey, String answer) {
        this.task = task;
        this.apikey = apikey;
        this.answer = answer;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
} 