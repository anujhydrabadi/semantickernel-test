package test.anuj.ai.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.google.gson.Gson;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.contextvariables.ContextVariableTypeConverter;
import com.microsoft.semantickernel.contextvariables.ContextVariableTypes;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.services.ServiceNotFoundException;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KernelInvoker {
  public static final String ENDPOINT = "https://facets-gpt4.openai.azure.com/";
  public static final String TOKEN = "";
  public static final String MODEL = "gpt-4o";
  private final ChatHistory history = new ChatHistory();
  private final Kernel kernel;

  public KernelInvoker() {
    kernel = chatCompletionKernel();
  }

  private static OpenAIAsyncClient openAIClient() {
    return new OpenAIClientBuilder()
        .endpoint(ENDPOINT)
        .credential(new AzureKeyCredential(TOKEN))
        .buildAsyncClient();
  }

  private Kernel chatCompletionKernel() {
    OpenAIAsyncClient client = openAIClient();
    ChatCompletionService chatCompletion =
        OpenAIChatCompletion.builder().withOpenAIAsyncClient(client).withModelId(MODEL).build();
    KernelPlugin lightsPlugin =
        KernelPluginFactory.createFromObject(new LightsPlugin(), "LightsPlugin");
    return Kernel.builder()
        .withAIService(ChatCompletionService.class, chatCompletion)
        .withPlugin(lightsPlugin)
        .build();
  }

  public void prompt(String prompt) {
    ContextVariableTypes.addGlobalConverter(
        ContextVariableTypeConverter.builder(LightModel.class)
            .toPromptString(new Gson()::toJson)
            .build());
    // Enable planning
    InvocationContext invocationContext =
        InvocationContext.builder()
            .withReturnMode(InvocationReturnMode.LAST_MESSAGE_ONLY)
            .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
            .withContextVariableConverter(
                ContextVariableTypeConverter.builder(LightModel.class)
                    .toPromptString(new Gson()::toJson)
                    .build())
            .build();
    ChatCompletionService chatCompletionService = null;
    try {
      chatCompletionService = kernel.getService(ChatCompletionService.class);
    } catch (ServiceNotFoundException e) {
      throw new RuntimeException(e);
    }

    // Initiate a back-and-forth chat
    // Collect user input
    System.out.print("User > ");
    // Add user input
    history.addUserMessage(prompt);
    List<ChatMessageContent<?>> results =
        chatCompletionService
            .getChatMessageContentsAsync(history, kernel, invocationContext)
            .block();
    for (ChatMessageContent<?> result : results) {
      // Print the results
      if (result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null) {
        System.out.println("Assistant > " + result);
      }
      // Add the message from the agent to the chat history
      history.addMessage(result);
    }
  }
}
