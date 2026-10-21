import { common } from '@kit.AbilityKit';
import { ClientModel } from '../model/ClientModel';
import { METHODS } from '../model/Profile';

@Entry
@Component
struct Index {
  @State model: ClientModel = new ClientModel();
  @State link: string = '';

  aboutToAppear(): void { this.model.initialize(getContext(this) as common.UIAbilityContext); }
  aboutToDisappear(): void { this.model.dispose(); }

  private status(): Resource {
    if (this.model.state === 'starting') { return $r('app.string.starting'); }
    if (this.model.state === 'stopping') { return $r('app.string.stopping'); }
    if (this.model.state === 'tunnel') { return $r('app.string.tunnel'); }
    if (this.model.state === 'unknown') { return $r('app.string.unknown'); }
    if (this.model.state === 'error') { return $r('app.string.error'); }
    return $r('app.string.idle');
  }

  private feedback(): Resource {
    if (this.model.feedback === 'import_ok') { return $r('app.string.import_ok'); }
    if (this.model.feedback === 'import_failed') { return $r('app.string.import_failed'); }
    if (this.model.feedback === 'field_error') { return $r('app.string.field_error'); }
    if (this.model.feedback === 'terms_required') { return $r('app.string.terms_required'); }
    if (this.model.feedback === 'permission_error') { return $r('app.string.permission_error'); }
    if (this.model.feedback === 'core_error') { return $r('app.string.core_error'); }
    return $r('app.string.save_error');
  }

  build() {
    Stack() {
      Column().width('100%').height('100%')
        .linearGradient({ angle: 135, colors: [['#EBFAFF', 0], ['#C2E6FA', 0.45], ['#DDD7FA', 1]] })
      Scroll() {
        Column({ space: 18 }) {
          Row() {
            Image($r('app.media.app_icon')).width(44).height(44).borderRadius(12)
            Column({ space: 4 }) {
              Text($r('app.string.app_name')).fontSize(25).fontWeight(FontWeight.Bold).fontColor('#142D50')
              Text($r('app.string.prototype')).fontSize(12).fontColor('#607994')
            }.alignItems(HorizontalAlign.Start).margin({ left: 12 })
          }.width('100%')
          Text($r('app.string.headline')).fontSize(30).fontWeight(FontWeight.Bold).fontColor('#142D50').width('100%')
          Text(this.status()).fontSize(16).fontColor('#365C80')
          Button({ type: ButtonType.Circle }) {
            Column({ space: 8 }) {
              Text($r('app.string.power')).fontSize(68).fontColor('#197BFF')
              Text(this.model.canDisconnect
                ? $r('app.string.disconnect') : $r('app.string.connect')).fontSize(16).fontColor('#17406A')
            }
          }.width(188).height(188).backgroundColor('#55FFFFFF')
            .backgroundBlurStyle(BlurStyle.Thin).border({ width: 2, color: '#EEFFFFFF', radius: 94 })
            .shadow({ radius: 24, color: '#284971A8', offsetY: 10 })
            .enabled(this.model.ready && this.model.state !== 'stopping')
            .accessibilityText(this.model.canDisconnect ? $r('app.string.disconnect') : $r('app.string.connect'))
            .onClick(() => { this.model.toggle(); })
          if (this.model.feedback.length > 0) {
            Text(this.feedback()).fontSize(14).fontColor('#7C364A').width('100%')
          }
          Column({ space: 12 }) {
            Text($r('app.string.import_title')).fontSize(18).fontWeight(FontWeight.Medium).width('100%')
            TextArea({ text: this.link, placeholder: $r('app.string.link_hint') })
              .height(85).maxLength(16384).onChange((value: string) => { this.link = value; })
              .accessibilityText($r('app.string.import_title'))
            Button($r('app.string.import_action')).width('100%').enabled(!this.model.busy())
              .onClick(() => { this.model.importLink(this.link); this.link = ''; })
          }.padding(18).width('100%').backgroundColor('#55FFFFFF').borderRadius(26)
            .border({ width: 1, color: '#DDFFFFFF', radius: 26 }).backgroundBlurStyle(BlurStyle.Thin)
          Column({ space: 12 }) {
            Text($r('app.string.node')).fontSize(18).fontWeight(FontWeight.Medium).width('100%')
            TextInput({ text: this.model.draft.name, placeholder: $r('app.string.name') })
              .onChange((value: string) => { this.model.field('name', value); }).accessibilityText($r('app.string.name'))
            TextInput({ text: this.model.draft.host, placeholder: $r('app.string.host') })
              .onChange((value: string) => { this.model.field('host', value); }).accessibilityText($r('app.string.host'))
            TextInput({ text: this.model.portText, placeholder: $r('app.string.port') }).type(InputType.Number)
              .onChange((value: string) => { this.model.field('port', value); }).accessibilityText($r('app.string.port'))
            Text($r('app.string.method')).width('100%').fontSize(13)
            Select(METHODS.map((method: string): SelectOption => { return { value: method }; }))
              .value(this.model.draft.method).width('100%')
              .onSelect((index: number) => { this.model.field('method', METHODS[index]); })
              .accessibilityText($r('app.string.method'))
            TextInput({ text: this.model.draft.password, placeholder: $r('app.string.password') }).type(InputType.Password)
              .onChange((value: string) => { this.model.field('password', value); }).accessibilityText($r('app.string.password'))
          }.padding(18).width('100%').backgroundColor('#55FFFFFF').borderRadius(26)
            .border({ width: 1, color: '#DDFFFFFF', radius: 26 }).backgroundBlurStyle(BlurStyle.Thin)
          Text($r('app.string.terms_title')).fontSize(16).fontWeight(FontWeight.Medium).width('100%')
          Text($r('app.string.terms_body')).fontSize(13).fontColor('#42617F').width('100%')
          Row() {
            Checkbox({ name: 'consent', group: 'terms' }).select(this.model.agreed)
              .onChange((agreed: boolean) => { this.model.agreed = agreed; })
              .accessibilityText($r('app.string.terms'))
            Text($r('app.string.terms')).fontSize(13).layoutWeight(1)
          }.width('100%')
          Button(this.model.canDisconnect
            ? $r('app.string.disconnect') : $r('app.string.connect')).width('100%')
            .enabled(this.model.ready && this.model.state !== 'stopping')
            .onClick(() => { this.model.toggle(); })
          Text($r('app.string.session_note')).fontSize(12).fontColor('#526D8A').width('100%')
          Text($r('app.string.local_note')).fontSize(12).fontColor('#526D8A').width('100%')
          Text($r('app.string.credit')).fontSize(12).fontColor('#526D8A')
        }.padding({ left: 22, right: 22, top: 30, bottom: 32 }).width('100%')
      }.width('100%').height('100%').scrollBar(BarState.Auto)
    }.width('100%').height('100%')
  }
}
