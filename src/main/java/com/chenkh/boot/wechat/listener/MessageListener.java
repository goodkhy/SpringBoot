package com.chenkh.boot.wechat.listener;

import com.chenkh.boot.wechat.service.WeixinService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@Slf4j
@RestController
@RequestMapping("/wechat/love")
public class MessageListener {

    @Resource
    WeixinService weixinService;


    //提交验证 -->发送get请求
    @ResponseBody
    @GetMapping(produces = "text/plain;charset=utf-8")
    public String authGet(@RequestParam(name = "signature", required = false) String signature,
                          @RequestParam(name = "timestamp", required = false) String timestamp,
                          @RequestParam(name = "nonce", required = false) String nonce,
                          @RequestParam(name = "echostr", required = false) String echostr) {
        return authGet(signature, timestamp, nonce, echostr,null);
    }

    @ResponseBody
    @GetMapping(value = "/{channel}", produces = "text/plain;charset=utf-8")
    public String authGet(@RequestParam(name = "signature", required = false) String signature,
                          @RequestParam(name = "timestamp", required = false) String timestamp,
                          @RequestParam(name = "nonce", required = false) String nonce,
                          @RequestParam(name = "echostr", required = false) String echostr,
                          @PathVariable(name = "channel") String channel) {
        log.info("\n接收到来自微信服务器的认证消息：[{}, {}, {}, {},{}]", signature, timestamp, nonce, echostr, channel);

        if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
            throw new IllegalArgumentException("请求参数非法，请核实!");
        }

        WxMpService wxMpService = weixinService;

        if (wxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }

        return "非法请求";
    }



    @ResponseBody
    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature,
                       @RequestParam("timestamp") String timestamp, @RequestParam("nonce") String nonce) {
        return post(requestBody, signature, encType, msgSignature, timestamp, nonce, null);
    }

    @ResponseBody
    @PostMapping(value = "/{channel}", produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @PathVariable(name = "channel") String channel) {
        try {
            log.info(
                    "\n接收微信请求：[signature=[{}], encType=[{}], msgSignature=[{}],"
                            + " timestamp=[{}], nonce=[{}], requestBody=[\n{}\n] {}",
                    signature, encType, msgSignature, timestamp, nonce, requestBody, channel);

            WeixinService wxMpService = weixinService;


            if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
                throw new IllegalArgumentException("非法请求，可能属于伪造的请求！");
            }

            String out = null;
            if (encType == null) {
                // 明文传输的消息
                WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
                WxMpXmlOutMessage outMessage = wxMpService.route(inMessage);
                if (outMessage == null) {
                    return "";
                }

                out = outMessage.toXml();
            } else if ("aes".equals(encType)) {
//                log.info("========================aes============================");
                // aes加密的消息
                WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody,
                        wxMpService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
                log.info("\n消息解密后内容为：\n{} ", inMessage.toString());
                WxMpXmlOutMessage outMessage = wxMpService.route(inMessage);
                if (outMessage == null) {
                    return "";
                }

                out = outMessage.toEncryptedXml(wxMpService.getWxMpConfigStorage());
            }

            log.info("\n组装回复信息：{}", out);

            return out;
        } catch (Throwable e) {
            log.error("", e);
        }
        return "";
    }

}
