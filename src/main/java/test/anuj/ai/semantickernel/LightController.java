package test.anuj.ai.semantickernel;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lights")
@RequiredArgsConstructor
public class LightController {
  private final KernelInvoker kernelInvoker;

  @PostMapping
  public void prompt(@RequestParam String prompt) {
    kernelInvoker.prompt(prompt);
  }
}
