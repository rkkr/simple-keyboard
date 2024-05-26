package rkr.simplekeyboard.inputmethod.latin;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.util.ArrayList;
import java.util.List;

public class GPTTranslator {
    private final String mErrUnableToTrans = "500";

    private final String mPromptUnableToTranslate = "If the model is unable to translate, return the code 500.\n";

    private LatinIME mLatinIME;

    private final String mDefaultSysPrompt = "Translate the following sentence to Indonesian with a casual tone." +
            "\n" +
            "example: \n" +
            "\n" +
            "user: , system: 500\n" +
            "\n" +
            "user: h, system: 500\n" +
            "\n" +
            "user: this is money, system: ini uang\n" +
            "\n" +
            "user: I'm eating at the cafe with your friend, system: gw lg makan di kafe bareng temen lo";

    public GPTTranslator(final LatinIME latinIME) {
        mLatinIME = latinIME;
    }

    private List<ChatMessage> generateChatMessages(String originalString) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("system", mPromptUnableToTranslate +mDefaultSysPrompt));
        chatMessages.add(new ChatMessage("user", originalString));
        return chatMessages;
    }

    private List<ChatCompletionChoice> getCompletionChoices(ChatCompletionRequest chatCompletionRequest) {
        OpenAiService openAiService = new OpenAiService(mLatinIME.mSettings.getCurrent().mOpenAIAPIKey);
        return openAiService.createChatCompletion(chatCompletionRequest).getChoices();
    }

    private ChatCompletionRequest buildChatCompletionRequest(String text){
        return ChatCompletionRequest.builder()
                .messages(generateChatMessages(text))
                .model(mLatinIME.mSettings.getCurrent().mOpenAIModel)
                .temperature((double) mLatinIME.mSettings.getCurrent().mOpenAITemperature)
                .build();
    }

    public String translate(String text) {
        // reject an empty string
        if (text.isEmpty()){
            return text;
        }
        try{
            for (ChatCompletionChoice choice : getCompletionChoices(buildChatCompletionRequest(text))) {
                String content = choice.getMessage().getContent();
                // 500 means the model is unable to translate
                if (content.equals(mErrUnableToTrans)){
                    return text;
                }
                return choice.getMessage().getContent();
            }
        }catch (Exception e){
            return "Exception:"+e.getMessage();
        }
        return "";
    }
}
