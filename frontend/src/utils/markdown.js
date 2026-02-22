import DOMPurify from "dompurify";
import hljs from "highlight.js/lib/core";
import bash from "highlight.js/lib/languages/bash";
import c from "highlight.js/lib/languages/c";
import cpp from "highlight.js/lib/languages/cpp";
import csharp from "highlight.js/lib/languages/csharp";
import go from "highlight.js/lib/languages/go";
import java from "highlight.js/lib/languages/java";
import javascript from "highlight.js/lib/languages/javascript";
import json from "highlight.js/lib/languages/json";
import kotlin from "highlight.js/lib/languages/kotlin";
import MarkdownIt from "markdown-it";
import texmath from "markdown-it-texmath";
import katex from "katex";
import python from "highlight.js/lib/languages/python";
import rust from "highlight.js/lib/languages/rust";
import sql from "highlight.js/lib/languages/sql";
import typescript from "highlight.js/lib/languages/typescript";
import xml from "highlight.js/lib/languages/xml";
import yaml from "highlight.js/lib/languages/yaml";

hljs.registerLanguage("bash", bash);
hljs.registerLanguage("sh", bash);
hljs.registerLanguage("c", c);
hljs.registerLanguage("cpp", cpp);
hljs.registerLanguage("c++", cpp);
hljs.registerLanguage("cxx", cpp);
hljs.registerLanguage("cs", csharp);
hljs.registerLanguage("csharp", csharp);
hljs.registerLanguage("go", go);
hljs.registerLanguage("java", java);
hljs.registerLanguage("js", javascript);
hljs.registerLanguage("javascript", javascript);
hljs.registerLanguage("json", json);
hljs.registerLanguage("kotlin", kotlin);
hljs.registerLanguage("kt", kotlin);
hljs.registerLanguage("py", python);
hljs.registerLanguage("python", python);
hljs.registerLanguage("rs", rust);
hljs.registerLanguage("rust", rust);
hljs.registerLanguage("sql", sql);
hljs.registerLanguage("ts", typescript);
hljs.registerLanguage("typescript", typescript);
hljs.registerLanguage("html", xml);
hljs.registerLanguage("xml", xml);
hljs.registerLanguage("yml", yaml);
hljs.registerLanguage("yaml", yaml);

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  highlight(code, language) {
    const lang = String(language || "").trim().toLowerCase();
    const hasLang = lang && hljs.getLanguage(lang);

    try {
      if (hasLang) {
        const highlighted = hljs.highlight(code, {
          language: lang,
          ignoreIllegals: true
        }).value;
        return `<pre><code class="hljs language-${lang}">${highlighted}</code></pre>`;
      }
      const auto = hljs.highlightAuto(code).value;
      return `<pre><code class="hljs language-plaintext">${auto}</code></pre>`;
    } catch {
      return `<pre><code class="hljs language-plaintext">${md.utils.escapeHtml(code)}</code></pre>`;
    }
  }
});

md.use(texmath, {
  engine: katex,
  delimiters: "dollars",
  katexOptions: {
    throwOnError: false
  }
});

const defaultLinkRender =
  md.renderer.rules.link_open ||
  ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options));

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet("target", "_blank");
  tokens[idx].attrSet("rel", "noopener noreferrer");
  return defaultLinkRender(tokens, idx, options, env, self);
};

export function renderMarkdown(source) {
  if (typeof source !== "string" || !source.trim()) {
    return "";
  }

  const unsafeHtml = md.render(source);
  return DOMPurify.sanitize(unsafeHtml, {
    USE_PROFILES: { html: true },
    ADD_ATTR: ["target", "rel", "class"]
  });
}
