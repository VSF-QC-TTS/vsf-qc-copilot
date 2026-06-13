import { useTranslations } from "next-intl";
import { setRequestLocale } from "next-intl/server";
import { CheckCircle } from "@phosphor-icons/react/dist/ssr";
import { Button } from "@/components/ui/button";

type Props = {
  params: Promise<{ locale: string }>;
};

export default async function Home({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);

  return <PlaceholderContent />;
}

function PlaceholderContent() {
  const t = useTranslations("placeholder");

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 p-8">
      <div className="flex items-center gap-3">
        <CheckCircle size={32} weight="fill" className="text-primary" />
        <h1 className="text-2xl font-semibold tracking-tight">
          {t("title")}
        </h1>
      </div>
      <p className="max-w-md text-center text-muted-foreground">
        {t("description")}
      </p>
      <div className="flex flex-wrap items-center justify-center gap-3">
        <Button>{t("buttons.primary")}</Button>
        <Button variant="secondary">{t("buttons.secondary")}</Button>
        <Button variant="outline">{t("buttons.outline")}</Button>
        <Button variant="ghost">{t("buttons.ghost")}</Button>
        <Button variant="destructive">{t("buttons.destructive")}</Button>
        <Button variant="link">{t("buttons.link")}</Button>
      </div>
    </main>
  );
}
