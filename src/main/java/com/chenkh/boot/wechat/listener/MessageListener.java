package com.chenkh.boot.wechat.listener;

import com.chenkh.boot.wechat.service.WeixinService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@Slf4j
@RestController
@RequestMapping("/wechat/chat")
public class MessageListener {

    @Resource
    WeixinService weixinService;

    @RequestMapping(method = RequestMethod.POST, produces = "application/xml; charset=UTF-8")
    public String receive(HttpServletRequest request,
            @RequestParam String signature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam(value = "encrypt_type", required = false) String encryptType,
            @RequestParam(value = "msg_signature",required = false) String msgSignature,
            @RequestBody String requestBody
    ) {
        log.info(
                "\n接收微信请求：[signature=[{}], encType=[{}], msgSignature=[{}],"
                        + " timestamp=[{}], nonce=[{}], requestBody=[\n{}\n] ",
                signature, encryptType, msgSignature, timestamp, nonce, requestBody);

        if (!weixinService.checkSignature(timestamp, nonce, signature)) {
            throw new IllegalArgumentException("非法请求，可能属于伪造的请求！");
        }

        String out = null;
        if (encryptType == null) {
            //明文传输的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
            WxMpXmlOutMessage outMessage = weixinService.route(inMessage);
            if (outMessage == null) {
                return "";
            }

            out = outMessage.toXml();
        } else if ("aes".equals(encryptType)) {
            //aes加密后的信息
            try{
                WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody,
                        weixinService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
                log.info("\n消息解密后内容为:\n{} ", inMessage.toString());
                WxMpXmlOutMessage outMessage = weixinService.route(inMessage);

                if (outMessage == null) {
                    return "";
                }

                out = outMessage.toEncryptedXml(weixinService.getWxMpConfigStorage());
            } catch (Exception e) {
                log.error("", e);
            }
        }

        log.info("\n组装回复信息：{}", out);

        return out;
    }

}
